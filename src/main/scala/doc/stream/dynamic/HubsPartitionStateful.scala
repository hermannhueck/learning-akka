package doc.stream.dynamic

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl._

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}


object HubsPartitionStateful extends App {

  implicit val system: ActorSystem = ActorSystem("SimpleGraph")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContext = system.dispatcher

  println("\n-----")


  //#partition-hub
  // A simple producer that publishes a new "message-" every second
  val producer = Source.tick(1.second, 1.second, "message")
    .zipWith(Source(1 to 100))((a, b) ⇒ s"$a-$b").take(5)

  // New instance of the partitioner function and its state is created
  // for each materialization of the PartitionHub.
  def roundRobin(): (PartitionHub.ConsumerInfo, String) ⇒ Long = {
    var i = -1L

    (info, elem) ⇒ {
      i += 1
      info.consumerIdByIdx((i % info.size).toInt)
    }
  }

  // Attach a PartitionHub Sink to the producer. This will materialize to a
  // corresponding Source.
  // (We need to use toMat and Keep.right since by default the materialized
  // value to the left is used)
  val runnableGraph: RunnableGraph[Source[String, NotUsed]] =
  producer.toMat(PartitionHub.statefulSink(
    () ⇒ roundRobin(),
    startAfterNrOfConsumers = 2, bufferSize = 256))(Keep.right)

  // By running/materializing the producer, we get back a Source, which
  // gives us access to the elements published by the producer.
  val fromProducer: Source[String, NotUsed] = runnableGraph.run()

  // Print out messages from the producer in two independent consumers
  val done1 = fromProducer.runForeach(msg ⇒ println("consumer1: " + msg))
  val done2 = fromProducer.runForeach(msg ⇒ println("consumer2: " + msg))

  Await.ready(done1, 6.seconds)
  Await.ready(done2, 6.seconds)



  Await.result(system.terminate(), 3.seconds)

  println("-----\n")
}
