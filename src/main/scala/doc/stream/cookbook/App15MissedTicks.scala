package doc.stream.cookbook

import akka.NotUsed
import akka.stream.scaladsl.{Flow, Source}

import scala.concurrent.Await
import scala.concurrent.duration._

object App15MissedTicks extends AbstractApp {

  def missedTicks[T]: Flow[T, Int, NotUsed] =
    Flow[T].conflateWithSeed(seed = _ => 0)((missedTicks, tick) => missedTicks + 1)
    //Flow[T].conflate((lastMessage, newMessage) => newMessage)

  Source(1 to 10)
    .throttle(1, 300.milliseconds)
    .via(missedTicks)
    .throttle(1, 900.milliseconds)
    .runForeach(println)

  Thread sleep 3000L


  Await.ready(system.terminate(), 3.seconds)
  println("-----\n")
}
