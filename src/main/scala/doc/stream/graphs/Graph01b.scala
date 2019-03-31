package doc.stream.graphs

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl._

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}


object Graph01b extends App {

  implicit val system: ActorSystem = ActorSystem("SimpleGraph")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContext = system.dispatcher

  println("\n-----")


  //#simple-graph-dsl

  private def shape(implicit builder: GraphDSL.Builder[NotUsed]): ClosedShape = {

    import GraphDSL.Implicits._

    val in = Source(1 to 10)
    val out = Sink foreach println

    val bcast: UniformFanOutShape[Int, Int] = builder.add(Broadcast[Int](2))
    val merge: UniformFanInShape[Int, Int] = builder.add(Merge[Int](2))

    val f1, f2, f3, f4 = Flow[Int].map(_ + 10)

    in ~> f1 ~> bcast ~> f2 ~> merge ~> f3 ~> out
    bcast ~> f4 ~> merge

    ClosedShape
  }

  val dsl: Graph[ClosedShape, NotUsed] = GraphDSL.create() {
    implicit builder: GraphDSL.Builder[NotUsed] => shape(builder)
  }

  val graph = RunnableGraph.fromGraph(dsl)

  graph.run()


  //Await.result(result, 3.seconds)
  Await.result(system.terminate(), 3.seconds)

  println("-----\n")
}
