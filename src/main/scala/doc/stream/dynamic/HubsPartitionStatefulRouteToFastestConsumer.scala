package doc.stream.dynamic

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl._

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}


object HubsPartitionStatefulRouteToFastestConsumer extends App {

  implicit val system: ActorSystem = ActorSystem("SimpleGraph")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContext = system.dispatcher

  println("\n-----")


  //#partition-hub-fastest
  val producer = Source(0 until 100)

  // ConsumerInfo.queueSize is the approximate number of buffered elements for a consumer.
  // Note that this is a moving target since the elements are consumed concurrently.
  val runnableGraph: RunnableGraph[Source[Int, NotUsed]] =
    producer.toMat(PartitionHub.statefulSink(
      () ⇒ (info, elem) ⇒ info.consumerIds.minBy(id ⇒ info.queueSize(id)),
      startAfterNrOfConsumers = 2, bufferSize = 16))(Keep.right)

  val fromProducer: Source[Int, NotUsed] = runnableGraph.run()

  val done1 = fromProducer.runForeach(msg ⇒ println("consumer1: " + msg))
  val done2 = fromProducer.throttle(10, 100.millis)
    .runForeach(msg ⇒ println("consumer2: " + msg))

  Await.ready(done1, 6.seconds)
  Await.ready(done2, 6.seconds)


  Await.result(system.terminate(), 3.seconds)

  println("-----\n")
}
