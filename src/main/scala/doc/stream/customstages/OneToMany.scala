package doc.stream.customstages

import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl._
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}


object OneToMany extends App {

  implicit val system: ActorSystem = ActorSystem("OneToMany")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContext = system.dispatcher

  println("\n-----")


  //#one-to-many
  class Duplicator[A] extends GraphStage[FlowShape[A, A]] {

    private val in = Inlet[A]("Duplicator.in")
    private val out = Outlet[A]("Duplicator.out")

    val shape: FlowShape[A, A] = FlowShape.of(in, out)

    override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
      new GraphStageLogic(shape) {
        // Again: note that all mutable state
        // MUST be inside the GraphStageLogic
        var lastElem: Option[A] = None

        setHandler(in, new InHandler {

          override def onPush(): Unit = {
            val elem = grab(in)
            lastElem = Some(elem)
            push(out, elem)
          }

          override def onUpstreamFinish(): Unit = {
            if (lastElem.isDefined) emit(out, lastElem.get)
            complete(out)
          }
        })

        setHandler(out, new OutHandler {

          override def onPull(): Unit = {
            if (lastElem.isDefined) {
              push(out, lastElem.get)
              lastElem = None
            } else {
              pull(in)
            }
          }
        })
      }
  }

  val duplicator = Flow.fromGraph(new Duplicator[Int])

  val result =
    Source(Vector(1, 2, 3))
      .via(duplicator)
      .runFold(Seq.empty[Int])((elem, acc) ⇒ elem :+ acc)

  println(Await.result(result, 3.seconds)) // should ===(Seq(1, 1, 2, 2, 3, 3))


  Await.result(system.terminate(), 3.seconds)

  println("-----\n")
}
