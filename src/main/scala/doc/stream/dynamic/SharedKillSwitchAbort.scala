package doc.stream.dynamic

import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl._

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}


object SharedKillSwitchAbort extends App {

  private def doSomethingElse(): Unit = Thread sleep 2200L

  implicit val system: ActorSystem = ActorSystem("SimpleGraph")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContext = system.dispatcher

  println("\n-----")


  //#shared-abort
  val countingSrc = Source(Stream.from(1)).delay(1.second)
  val lastSnk = Sink.last[Int]
  val sharedKillSwitch = KillSwitches.shared("my-kill-switch")

  val last1 = countingSrc.via(sharedKillSwitch.flow).runWith(lastSnk)
  val last2 = countingSrc.via(sharedKillSwitch.flow).runWith(lastSnk)

  val error = new RuntimeException("boom!")
  sharedKillSwitch.abort(error)

  println(Await.result(last1.failed, 1.second)) // shouldBe error
  println(Await.result(last2.failed, 1.second)) // shouldBe error


  Await.result(system.terminate(), 3.seconds)

  println("-----\n")
}
