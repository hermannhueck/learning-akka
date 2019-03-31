package doc.stream.errorhandling

import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl._

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}


object LogError extends App {

  implicit val system: ActorSystem = ActorSystem("LogError")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContext = system.dispatcher

  println("\n-----")


  val result = Source(-5 to 5)
    .map(1 / _) //throwing ArithmeticException: / by zero
    .log("error logging")
    .runWith(Sink.ignore)

  Await.ready(result, 1.second)


  Await.ready(system.terminate(), 3.seconds)

  println("-----\n")
}
