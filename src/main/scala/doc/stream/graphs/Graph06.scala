package doc.stream.graphs

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl._

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}


object Graph06 extends App {

  implicit val system: ActorSystem = ActorSystem("SimpleGraph")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContext = system.dispatcher

  println("\n-----")


  //#flow-from-partial-graph-dsl
  val pairUpWithToString: Flow[Int, (Int, String), NotUsed] =
    Flow.fromGraph(GraphDSL.create() { implicit builder ⇒

      import GraphDSL.Implicits._

      // prepare graph elements
      val broadcast = builder.add(Broadcast[Int](2))
      val zip = builder.add(Zip[Int, String]())

      // connect the graph
      broadcast.out(0).map(identity) ~> zip.in0
      broadcast.out(1).map(_.toString) ~> zip.in1

      // expose ports
      FlowShape(broadcast.in, zip.out)
    })

  val (_, matSink: Future[(Int, String)]) =
    pairUpWithToString.runWith(Source(List(1)), Sink.head)

  println(Await.result(matSink, 300.millis)) // should equal(1 -> "1")



  //Await.result(result, 3.seconds)
  Await.result(system.terminate(), 3.seconds)

  println("-----\n")
}
