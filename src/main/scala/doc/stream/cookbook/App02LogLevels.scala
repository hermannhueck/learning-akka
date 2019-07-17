package doc.stream.cookbook

import akka.event.Logging
import akka.stream.Attributes
import akka.stream.scaladsl.{Sink, Source}

import scala.concurrent.Await
import scala.concurrent.duration._

object App02LogLevels extends AkkaStreamApp {

  val mySource = Source(List("1", "2", "3"))

  def analyse(s: String) = s

  // customise log levels
  val loggedSource = mySource
    .log("before-map")
    .withAttributes(Attributes
      .logLevels(
        onElement = Logging.WarningLevel,
        onFinish = Logging.InfoLevel,
        onFailure = Logging.DebugLevel))
    .map(analyse)

  val result = loggedSource.runWith(Sink.ignore)

  Await.ready(result, 1.second)


  Await.ready(system.terminate(), 3.seconds)
  println("-----\n")
}
