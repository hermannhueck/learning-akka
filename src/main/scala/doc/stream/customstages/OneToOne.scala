package doc.stream.customstages

import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl._
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}


object OneToOne extends App {

  implicit val system: ActorSystem = ActorSystem("OneToOne")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContext = system.dispatcher

  println("\n-----")


  //#one-to-one
  class Map[A, B](f: A ⇒ B) extends GraphStage[FlowShape[A, B]] {

    private val in = Inlet[A]("Map.in")
    private val out = Outlet[B]("Map.out")

    override val shape: FlowShape[A, B] = FlowShape.of(in, out)

    override def createLogic(attr: Attributes): GraphStageLogic =

      new GraphStageLogic(shape) {

        setHandler(in, new InHandler {
          override def onPush(): Unit = {
            push(out, f(grab(in)))
          }
        })

        setHandler(out, new OutHandler {
          override def onPull(): Unit = {
            pull(in)
          }
        })
      }
  }

  val stringLength = Flow.fromGraph(new Map[String, Int](_.length))

  val result =
    Source(Vector("one", "two", "three"))
      .via(stringLength)
      .runFold(Seq.empty[Int])((elem, acc) ⇒ elem :+ acc)

  println(Await.result(result, 3.seconds)) // should ===(Seq(3, 3, 5))


  Await.result(system.terminate(), 3.seconds)

  println("-----\n")
}
