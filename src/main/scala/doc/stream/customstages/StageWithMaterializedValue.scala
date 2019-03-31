package doc.stream.customstages

import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl._
import akka.stream.stage.{GraphStageLogic, GraphStageWithMaterializedValue, InHandler, OutHandler}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future, Promise}


object StageWithMaterializedValue extends App {

  implicit val system: ActorSystem = ActorSystem("StageWithMaterializedValue")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContext = system.dispatcher

  println("\n-----")


  //#materialized
  class FirstValue[A] extends GraphStageWithMaterializedValue[FlowShape[A, A], Future[A]] {

    private val in = Inlet[A]("FirstValue.in")
    private val out = Outlet[A]("FirstValue.out")

    val shape: FlowShape[A, A] = FlowShape.of(in, out)

    override def createLogicAndMaterializedValue(inheritedAttributes: Attributes): (GraphStageLogic, Future[A]) = {

      val promise = Promise[A]()

      val logic = new GraphStageLogic(shape) {

        setHandler(in, new InHandler {
          override def onPush(): Unit = {
            val elem = grab(in)
            promise.success(elem)
            push(out, elem)

            // replace handler with one that only forwards elements
            setHandler(in, new InHandler {
              override def onPush(): Unit = {
                push(out, grab(in))
              }
            })
          }
        })

        setHandler(out, new OutHandler {
          override def onPull(): Unit = {
            pull(in)
          }
        })

      }

      (logic, promise.future)
    }
  }

  val flow = Source(Vector(1, 2, 3))
    .viaMat(new FirstValue)(Keep.right)
    .to(Sink.ignore)

  val result: Future[Int] = flow.run()

  println(Await.result(result, 3.seconds)) // should ===(1)


  Await.result(system.terminate(), 3.seconds)

  println("-----\n")
}
