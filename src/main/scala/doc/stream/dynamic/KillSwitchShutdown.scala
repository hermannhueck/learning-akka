package doc.stream.dynamic

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl._

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}


object KillSwitchShutdown extends App {

  private def doSomethingElse(): Unit = Thread sleep 1100L

  implicit val system: ActorSystem = ActorSystem("SimpleGraph")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContext = system.dispatcher

  println("\n-----")


  //#unique-shutdown
  val countingSrc: Source[Int, NotUsed] = Source(Stream.from(1)).delay(1.second, DelayOverflowStrategy.backpressure)
  val lastSnk: Sink[Int, Future[Int]] = Sink.last[Int]

  val (killSwitch, last) = countingSrc
    .viaMat(KillSwitches.single)(Keep.right)
    .toMat(lastSnk)(Keep.both)
    .run()

  doSomethingElse()

  killSwitch.shutdown()

  println(Await.result(last, 1.second)) // shouldBe 2


  Await.result(system.terminate(), 3.seconds)

  println("-----\n")
}
