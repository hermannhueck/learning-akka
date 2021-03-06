package doc.stream.graphs

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl._

import scala.collection.immutable
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}


object Graph09 extends App {

  implicit val system: ActorSystem = ActorSystem("SimpleGraph")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContext = system.dispatcher

  println("\n-----")


  //#graph-dsl-components-shape
  // A shape represents the input and output ports of a reusable
  // processing module
  case class PriorityWorkerPoolShape[In, Out](jobsIn: Inlet[In],
                                              priorityJobsIn: Inlet[In],
                                              resultsOut: Outlet[Out]) extends Shape {

    // It is important to provide the list of all input and output
    // ports with a stable order. Duplicates are not allowed.
    override val inlets: immutable.Seq[Inlet[_]] =
      jobsIn :: priorityJobsIn :: Nil
    override val outlets: immutable.Seq[Outlet[_]] =
      resultsOut :: Nil

    // A Shape must be able to create a copy of itself. Basically
    // it means a new instance with copies of the ports
    override def deepCopy() = PriorityWorkerPoolShape(
      jobsIn.carbonCopy(),
      priorityJobsIn.carbonCopy(),
      resultsOut.carbonCopy())

  }

  //#graph-dsl-components-shape

  //#graph-dsl-components-create
  object PriorityWorkerPool {

    def apply[In, Out](worker: Flow[In, Out, Any],
                       workerCount: Int): Graph[PriorityWorkerPoolShape[In, Out], NotUsed] = {

      GraphDSL.create() { implicit b ⇒
        import GraphDSL.Implicits._

        val priorityMerge = b.add(MergePreferred[In](1))
        val balance = b.add(Balance[In](workerCount))
        val resultsMerge = b.add(Merge[Out](workerCount))

        // After merging priority and ordinary jobs, we feed them to the balancer
        priorityMerge ~> balance

        // Wire up each of the outputs of the balancer to a worker flow
        // then merge them back
        for (i ← 0 until workerCount)
          balance.out(i) ~> worker ~> resultsMerge.in(i)

        // We now expose the input ports of the priorityMerge and the output
        // of the resultsMerge as our PriorityWorkerPool ports
        // -- all neatly wrapped in our domain specific Shape
        PriorityWorkerPoolShape(
          jobsIn = priorityMerge.in(0),
          priorityJobsIn = priorityMerge.preferred,
          resultsOut = resultsMerge.out)
      }

    }

  }

  //#graph-dsl-components-create

  //def println(s: Any): Unit = ()

  //#graph-dsl-components-use
  val worker1 = Flow[String].map("step 1 " + _)
  val worker2 = Flow[String].map("step 2 " + _)

  RunnableGraph.fromGraph(GraphDSL.create() { implicit b ⇒
    import GraphDSL.Implicits._

    val priorityPool1 = b.add(PriorityWorkerPool(worker1, 4))
    val priorityPool2 = b.add(PriorityWorkerPool(worker2, 2))

    Source(1 to 100).map("job: " + _) ~> priorityPool1.jobsIn
    Source(1 to 100).map("priority job: " + _) ~> priorityPool1.priorityJobsIn

    priorityPool1.resultsOut ~> priorityPool2.jobsIn
    Source(1 to 100).map("one-step, priority " + _) ~> priorityPool2.priorityJobsIn

    priorityPool2.resultsOut ~> Sink.foreach(println)
    ClosedShape
  }).run()
  //#graph-dsl-components-use

  //#graph-dsl-components-shape2
  import FanInShape.{Init, Name}

  class PriorityWorkerPoolShape2[In, Out](_init: Init[Out] = Name("PriorityWorkerPool"))
    extends FanInShape[Out](_init) {

    protected override def construct(i: Init[Out]) = new PriorityWorkerPoolShape2(i)

    val jobsIn: Inlet[In] = newInlet[In]("jobsIn")
    val priorityJobsIn: Inlet[In] = newInlet[In]("priorityJobsIn")
    // Outlet[Out] with name "out" is automatically created
  }

  //#graph-dsl-components-shape2


  //Await.result(result, 3.seconds)
  Await.result(system.terminate(), 3.seconds)

  println("-----\n")
}
