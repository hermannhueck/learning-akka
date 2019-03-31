package doc.stream.customstages

import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl._
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}


object ManyToOne extends App {

  implicit val system: ActorSystem = ActorSystem("ManyToOne")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContext = system.dispatcher

  println("\n-----")


  //#many-to-one
  class Filter[A](p: A ⇒ Boolean) extends GraphStage[FlowShape[A, A]] {

    private val in = Inlet[A]("Filter.in")
    private val out = Outlet[A]("Filter.out")

    val shape: FlowShape[A, A] = FlowShape.of(in, out)

    override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =

      new GraphStageLogic(shape) {

        setHandler(in, new InHandler {
          override def onPush(): Unit = {
            val elem = grab(in)
            if (p(elem))
              push(out, elem)
            else
              pull(in)
          }
        })

        setHandler(out, new OutHandler {
          override def onPull(): Unit = {
            pull(in)
          }
        })
      }
  }

  val evenFilter = Flow.fromGraph(new Filter[Int](_ % 2 == 0))

  val result =
    Source(Vector(1, 2, 3, 4, 5, 6))
      .via(evenFilter)
      .runFold(Seq.empty[Int])((elem, acc) ⇒ elem :+ acc)

  println(Await.result(result, 3.seconds)) // should ===(Seq(2, 4, 6))


  Await.result(system.terminate(), 3.seconds)

  println("-----\n")
}
