package doc.stream.cookbook

import akka.NotUsed
import akka.stream.scaladsl.{Flow, Source}

import scala.concurrent.Await
import scala.concurrent.duration._

object App14SimpleDrop extends AbstractApp {

  def droppyStream[T]: Flow[T, T, NotUsed] =
    Flow[T].conflate((lastMessage, newMessage) => newMessage)

  Source(1 to 10)
    .throttle(1, 300.milliseconds)
    .via(droppyStream)
    .throttle(1, 900.milliseconds)
    .runForeach(println)

  Thread sleep 3000L


  Await.ready(system.terminate(), 3.seconds)
  println("-----\n")
}
