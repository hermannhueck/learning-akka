package doc.stream.graphs

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl._

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}


object Graph04a extends App {

  implicit val system: ActorSystem = ActorSystem("SimpleGraph")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContext = system.dispatcher

  println("\n-----")


  //#simple-partial-graph-dsl
  val pickMaxOfThree = GraphDSL.create() { implicit b ⇒

    import GraphDSL.Implicits._

    val zip1: FanInShape2[Int, Int, Int] = b.add(ZipWith[Int, Int, Int](math.max _))
    val zip2: FanInShape2[Int, Int, Int] = b.add(ZipWith[Int, Int, Int](math.max _))
    zip1.out ~> zip2.in0

    UniformFanInShape(zip2.out, zip1.in0, zip1.in1, zip2.in1)
  }

  val resultSink = Sink.head[Int]

  val g = RunnableGraph.fromGraph(GraphDSL.create(resultSink) { implicit b ⇒ sink ⇒

    import GraphDSL.Implicits._

    // importing the partial graph will return its shape (inlets & outlets)
    val pm3: UniformFanInShape[Int, Int] = b.add(pickMaxOfThree)

    Source.single(1) ~> pm3.in(0)
    Source.single(2) ~> pm3.in(1)
    Source.single(3) ~> pm3.in(2)
    pm3.out ~> sink.in

    ClosedShape
  })

  val max: Future[Int] = g.run()
  println(Await.result(max, 300.millis)) // should equal(3)


  //Await.result(result, 3.seconds)
  Await.result(system.terminate(), 3.seconds)

  println("-----\n")
}
