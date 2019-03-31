package doc.stream.customstages

import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl._
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler, TimerGraphStageLogic}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}


object GraphStageWithTimer extends App {

  implicit val system: ActorSystem = ActorSystem("GraphStageWithTimer")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContext = system.dispatcher

  println("\n-----")


  //#timed
  // each time an event is pushed through it will trigger a period of silence
  class TimedGate[A](silencePeriod: FiniteDuration) extends GraphStage[FlowShape[A, A]] {

    private val in = Inlet[A]("TimedGate.in")
    private val out = Outlet[A]("TimedGate.out")

    val shape: FlowShape[A, A] = FlowShape.of(in, out)

    override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =

      new TimerGraphStageLogic(shape) {

        var open = false

        setHandler(in, new InHandler {
          override def onPush(): Unit = {
            val elem = grab(in)
            if (open)
              pull(in)
            else {
              push(out, elem)
              open = true
              scheduleOnce(None, silencePeriod)
            }
          }
        })

        setHandler(out, new OutHandler {
          override def onPull(): Unit = {
            pull(in)
          }
        })

        override protected def onTimer(timerKey: Any): Unit = {
          open = false
        }
      }
  }

  val result =
    Source(Vector(1, 2, 3))
      .via(new TimedGate[Int](2.second))
      .takeWithin(250.millis)
      .runFold(Seq.empty[Int])((elem, acc) ⇒ elem :+ acc)

  println(Await.result(result, 3.seconds)) // should ===(Seq(1))


  Await.result(system.terminate(), 3.seconds)

  println("-----\n")
}
