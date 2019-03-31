package doc.stream.customstages

import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl._
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}

import scala.collection.mutable
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}


object DetachedGraphStage extends App {

  implicit val system: ActorSystem = ActorSystem("DetachedGraphStage")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContext = system.dispatcher

  println("\n-----")


  //#detached
  class TwoBuffer[A] extends GraphStage[FlowShape[A, A]] {

    private val in = Inlet[A]("TwoBuffer.in")
    private val out = Outlet[A]("TwoBuffer.out")

    val shape: FlowShape[A, A] = FlowShape.of(in, out)

    override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =

      new GraphStageLogic(shape) {

        private val buffer = mutable.Queue[A]()
        private def bufferFull = buffer.size == 2
        var downstreamWaiting = false

        override def preStart(): Unit = {
          // a detached stage needs to start upstream demand
          // itself as it is not triggered by downstream demand
          pull(in)
        }

        setHandler(in, new InHandler {

          override def onPush(): Unit = {
            val elem = grab(in)
            buffer.enqueue(elem)
            if (downstreamWaiting) {
              downstreamWaiting = false
              val bufferedElem = buffer.dequeue()
              push(out, bufferedElem)
            }
            if (!bufferFull) {
              pull(in)
            }
          }

          override def onUpstreamFinish(): Unit = {
            if (buffer.nonEmpty) {
              // emit the rest if possible
              emitMultiple(out, buffer.toIterator)
            }
            completeStage()
          }
        })

        setHandler(out, new OutHandler {

          override def onPull(): Unit = {
            if (buffer.isEmpty) {
              downstreamWaiting = true
            } else {
              val elem = buffer.dequeue
              push(out, elem)
            }
            if (!bufferFull && !hasBeenPulled(in)) {
              pull(in)
            }
          }
        })
      }

  }
  //#detached

  // tests:
  val result1 = Source(Vector(1, 2, 3))
    .via(new TwoBuffer)
    .runFold(Vector.empty[Int])((acc, n) ⇒ acc :+ n)

  println(Await.result(result1, 3.seconds)) // should ===(Vector(1, 2, 3))


  Await.result(system.terminate(), 3.seconds)

  println("-----\n")
}
