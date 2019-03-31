package doc.stream.customstages

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl._

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}


object CustomSink extends App {

  implicit val system: ActorSystem = ActorSystem("CustomSink")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContext = system.dispatcher

  println("\n-----")


  //#custom-sink-example
  import akka.stream.Attributes
  import akka.stream.Inlet
  import akka.stream.SinkShape
  import akka.stream.stage.GraphStage
  import akka.stream.stage.GraphStageLogic
  import akka.stream.stage.InHandler

  class StdoutSink extends GraphStage[SinkShape[Int]] {
    val in: Inlet[Int] = Inlet("StdoutSink")
    override val shape: SinkShape[Int] = SinkShape(in)

    override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
      new GraphStageLogic(shape) {

        // This requests one element at the Sink startup.
        override def preStart(): Unit = pull(in)

        setHandler(in, new InHandler {
          override def onPush(): Unit = {
            println(grab(in))
            pull(in)
          }
        })
      }
  }

  Source(List(0, 1, 2)).runWith(Sink.fromGraph(new StdoutSink))


  Await.result(system.terminate(), 3.seconds)

  println("-----\n")
}
