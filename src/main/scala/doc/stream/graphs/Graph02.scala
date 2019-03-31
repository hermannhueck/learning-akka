package doc.stream.graphs

import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl._

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}


object Graph02 extends App {

  implicit val system: ActorSystem = ActorSystem("SimpleGraph")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContext = system.dispatcher

  println("\n-----")


  val topHeadSink = Sink.head[Int]
  val bottomHeadSink = Sink.head[Int]
  val sharedDoubler = Flow[Int].map(_ * 2)

  //#graph-dsl-reusing-a-flow

  val g: RunnableGraph[(Future[Int], Future[Int])] =
    RunnableGraph.fromGraph(GraphDSL.create(topHeadSink, bottomHeadSink)((_, _)) { implicit builder =>

      (topHS: SinkShape[Int], bottomHS: SinkShape[Int]) =>

        import GraphDSL.Implicits._

        val broadcast = builder.add(Broadcast[Int](2))

        Source.single(1) ~> broadcast

        broadcast ~> sharedDoubler ~> topHS
        broadcast ~> sharedDoubler ~> bottomHS

        ClosedShape
    })

  val (topFuture, bottomFuture) = g.run()

  println(Await.result(topFuture, 300.millis))
  println(Await.result(bottomFuture, 300.millis))



  //Await.result(result, 3.seconds)
  Await.result(system.terminate(), 3.seconds)

  println("-----\n")
}
