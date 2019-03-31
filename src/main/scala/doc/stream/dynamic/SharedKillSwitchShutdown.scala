package doc.stream.dynamic

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl._

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}


object SharedKillSwitchShutdown extends App {

  private def doSomethingElse(): Unit = Thread sleep 2200L

  implicit val system: ActorSystem = ActorSystem("SimpleGraph")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContext = system.dispatcher

  println("\n-----")


  //#shared-shutdown
  val countingSrc = Source(Stream.from(1)).delay(1.second, DelayOverflowStrategy.backpressure)
  val lastSnk = Sink.last[Int]
  val sharedKillSwitch = KillSwitches.shared("my-kill-switch")

  val last = countingSrc
    .via(sharedKillSwitch.flow)
    .runWith(lastSnk)

  val delayedLast = countingSrc
    .delay(1.second, DelayOverflowStrategy.backpressure)
    .via(sharedKillSwitch.flow)
    .runWith(lastSnk)

  doSomethingElse()

  sharedKillSwitch.shutdown()

  println(Await.result(last, 1.second)) // shouldBe 2
  println(Await.result(delayedLast, 1.second)) // shouldBe 1


  Await.result(system.terminate(), 3.seconds)

  println("-----\n")
}
