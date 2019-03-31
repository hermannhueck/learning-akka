package doc.stream.cookbook

import akka.event.{Logging, LoggingAdapter}
import akka.stream.scaladsl.{Sink, Source}

import scala.concurrent.Await
import scala.concurrent.duration._

object App03LogAdapter extends AbstractApp {

  val mySource = Source(List("1", "2", "3"))

  def analyse(s: String) = s

  // or provide custom logging adapter
  implicit val adapter: LoggingAdapter = Logging(system, "customLogger")

  val loggedSource = mySource.log("custom")

  val result = loggedSource.runWith(Sink.ignore)

  Await.ready(result, 1.second)


  Await.ready(system.terminate(), 3.seconds)
  println("-----\n")
}
