package doc.stream.customstages

import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl._
import akka.stream.stage.{AsyncCallback, GraphStage, GraphStageLogic, InHandler, OutHandler}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future, Promise}


object AsyncSideChannel extends App {

  implicit val system: ActorSystem = ActorSystem("AsyncSideChannel")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContext = system.dispatcher

  println("\n-----")


  //#async-side-channel
  // will close upstream in all materializations of the graph stage instance
  // when the future completes
  class KillSwitch[A](switch: Future[Unit]) extends GraphStage[FlowShape[A, A]] {

    private val in = Inlet[A]("KillSwitch.in")
    private val out = Outlet[A]("KillSwitch.out")

    val shape: FlowShape[A, A] = FlowShape.of(in, out)

    override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
      new GraphStageLogic(shape) {

        override def preStart(): Unit = {
          val callbackHandler: Unit => Unit = _ => completeStage()
          val callback: AsyncCallback[Unit] = getAsyncCallback[Unit](callbackHandler)
          switch.foreach(callback.invoke)
        }

        setHandler(in, new InHandler {
          override def onPush(): Unit = push(out, grab(in))
        })

        setHandler(out, new OutHandler {
          override def onPull(): Unit = pull(in)
        })
      }
  }
  //#async-side-channel

  // tests:

  val switch = Promise[Unit]()
  val duplicator = Flow.fromGraph(new KillSwitch[Int](switch.future))

  Source(Stream.from(0))
    .throttle(1, 50.milliseconds)
    .via(duplicator)
    .to(Sink.foreach(println))
    .withAttributes(Attributes.inputBuffer(1, 1))
    .run()

  Thread sleep 1000L

  switch.success(Unit) // completes the stream


  /*
    val in = TestPublisher.probe[Int]()
    val out = TestSubscriber.probe[Int]()

    Source.fromPublisher(in)
      .via(duplicator)
      .to(Sink.fromSubscriber(out))
      .withAttributes(Attributes.inputBuffer(1, 1))
      .run()

    val sub = in.expectSubscription()

    out.request(1)

    sub.expectRequest()
    sub.sendNext(1)

    out.expectNext(1)

    switch.success(Unit)

    out.expectComplete()
  */


  Await.result(system.terminate(), 3.seconds)

  println("-----\n")
}
