package doc.stream.integration.actors

import akka.Done
import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl._

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}


object SourceQueue extends App {

  implicit val system: ActorSystem = ActorSystem("SourceQueue")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContext = system.dispatcher

  println("\n-----")


  //#source-queue
  val bufferSize = 10
  val elementsToProcess = 5

  val queue: SourceQueueWithComplete[Int] =
    Source
      .queue[Int](bufferSize, OverflowStrategy.backpressure)
      .throttle(elementsToProcess, 3.second)
      .map(x ⇒ x * x)
      .toMat(Sink.foreach(x ⇒ println(s"completed $x")))(Keep.left)
      .run()

  val source = Source(1 to 10)

  val done: Future[Done] = source.mapAsync(1)(x ⇒ {
    queue.offer(x).map {
      case QueueOfferResult.Enqueued ⇒ println(s"enqueued $x")
      case QueueOfferResult.Dropped ⇒ println(s"dropped $x")
      case QueueOfferResult.Failure(ex) ⇒ println(s"Offer failed ${ex.getMessage}")
      case QueueOfferResult.QueueClosed ⇒ println("Source Queue closed")
    }
  }).runWith(Sink.ignore)

  Await.ready(done, 3.seconds)


  Await.result(system.terminate(), 3.seconds)

  println("-----\n")
}
