package doc.stream.errorhandling

import akka.actor.ActorSystem
import akka.event.Logging
import akka.stream._
import akka.stream.scaladsl._

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}


object LogLevels extends App {

  implicit val system: ActorSystem = ActorSystem("LogLevels")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContext = system.dispatcher

  println("\n-----")


  val result = Source(-5 to 5)
    .map(1 / _) //throwing ArithmeticException: / by zero
    .log("error logging")
    .withAttributes(
      Attributes.logLevels(
        onElement = Logging.WarningLevel,
        onFinish = Logging.InfoLevel,
        onFailure = Logging.DebugLevel
      )
    )
    .runWith(Sink.ignore)

  Await.ready(result, 1.second)


  Await.ready(system.terminate(), 3.seconds)

  println("-----\n")
}
