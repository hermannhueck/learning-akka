package doc.stream.dynamic

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl._

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}


object HubsMerge extends App {

  implicit val system: ActorSystem = ActorSystem("SimpleGraph")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContext = system.dispatcher

  println("\n-----")


  //#merge-hub
  // A simple consumer that will print to the console for now
  val consumer = Sink.foreach(println)

  // Attach a MergeHub Source to the consumer. This will materialize to a
  // corresponding Sink.
  val runnableGraph: RunnableGraph[Sink[String, NotUsed]] =
  MergeHub.source[String](perProducerBufferSize = 16).to(consumer)

  // By running/materializing the consumer we get back a Sink, and hence
  // now have access to feed elements into it. This Sink can be materialized
  // any number of times, and every element that enters the Sink will
  // be consumed by our consumer.
  val toConsumer: Sink[String, NotUsed] = runnableGraph.run()

  // Feeding two independent sources into the hub.
  Source.single("Hello!").runWith(toConsumer)
  Source.single("Hub!").runWith(toConsumer)


  Await.result(system.terminate(), 3.seconds)

  println("-----\n")
}
