package doc.stream.integration.actors

import akka.NotUsed
import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.stream._
import akka.stream.scaladsl._
import akka.testkit.TestProbe

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}


object SinkActorRefWithAck extends App {

  implicit val system: ActorSystem = ActorSystem("SinkActorRefWithAck")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContext = system.dispatcher

  println("\n-----")


  //#actorRefWithAck-actor
  object AckingReceiver {
    case object Ack
    case object StreamInitialized
    case object StreamCompleted
    final case class StreamFailure(ex: Throwable)
  }

  class AckingReceiver(probe: ActorRef, ackWith: Any) extends Actor with ActorLogging {
    import AckingReceiver._

    def receive: Receive = {

      case StreamInitialized =>
        log.info("Stream initialized!")
        probe ! "Stream initialized!"
        sender() ! Ack // ack to allow the stream to proceed sending more elements

      case el: String =>
        log.info("Received element: {}", el)
        probe ! el
        sender() ! Ack // ack to allow the stream to proceed sending more elements

      case StreamCompleted =>
        log.info("Stream completed!")
        probe ! "Stream completed!"

      case StreamFailure(ex) =>
        log.error(ex, "Stream failed!")
        probe ! "Stream failed!"
    }
  }

  //#actorRefWithAck
  val words: Source[String, NotUsed] =
    Source(List("hello", "hi"))

  // sent from actor to stream to "ack" processing of given element
  val AckMessage = AckingReceiver.Ack

  // sent from stream to actor to indicate start, end or failure of stream:
  val InitMessage = AckingReceiver.StreamInitialized
  val OnCompleteMessage = AckingReceiver.StreamCompleted
  val onErrorMessage = (ex: Throwable) ⇒ AckingReceiver.StreamFailure(ex)

  val probe = TestProbe()
  val receiver = system.actorOf(Props(new AckingReceiver(probe.ref, ackWith = AckMessage)))

  val sink = Sink.actorRefWithAck(
    receiver,
    onInitMessage = InitMessage,
    ackMessage = AckMessage,
    onCompleteMessage = OnCompleteMessage,
    onFailureMessage = onErrorMessage
  )

  words
    .map(_.toLowerCase)
    .runWith(sink)

  probe.expectMsg("Stream initialized!")
  probe.expectMsg("hello")
  probe.expectMsg("hi")
  probe.expectMsg("Stream completed!")

  //Await.ready(result, 1.second)


  Await.result(system.terminate(), 3.seconds)

  println("-----\n")
}
