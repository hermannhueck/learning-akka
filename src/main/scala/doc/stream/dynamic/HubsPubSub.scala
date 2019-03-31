package doc.stream.dynamic

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl._

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}


object HubsPubSub extends App {

  implicit val system: ActorSystem = ActorSystem("SimpleGraph")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContext = system.dispatcher

  println("\n-----")


  //#pub-sub
  // Obtain a Sink and Source which will publish and receive from the "bus" respectively.
  val (sink, source) =
  MergeHub.source[String](perProducerBufferSize = 16)
    .toMat(BroadcastHub.sink(bufferSize = 256))(Keep.both)
    .run()

  // Ensure that the Broadcast output is dropped if there are no listening parties.
  // If this dropping Sink is not attached, then the broadcast hub will not drop any
  // elements itself when there are no subscribers, backpressuring the producer instead.
  source.runWith(Sink.ignore)

  // We create now a Flow that represents a publish-subscribe channel using the above
  // started stream as its "topic". We add two more features, external cancellation of
  // the registration and automatic cleanup for very slow subscribers.
  val busFlow: Flow[String, String, UniqueKillSwitch] =
  Flow.fromSinkAndSource(sink, source)
    .joinMat(KillSwitches.singleBidi[String, String])(Keep.right)
    .backpressureTimeout(3.seconds)

  val switch: UniqueKillSwitch =
    Source.repeat("Hello world!")
      .viaMat(busFlow)(Keep.right)
      .to(Sink.foreach(println))
      .run()

  Thread sleep 1000L

  // Shut down externally
  switch.shutdown()


  Await.result(system.terminate(), 3.seconds)

  println("-----\n")
}
