package doc.stream.errorhandling

import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl._

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}


object SupervisionStrategyDefaultsToStop extends App {

  implicit val system: ActorSystem = ActorSystem("SupervisionStrategyDefaultsToStop")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContext = system.dispatcher

  println("\n-----")


  val source = Source(0 to 5).map(100 / _)
  val result = source.runWith(Sink.fold(0)(_ + _))
  // division by zero will fail the stream and the
  // result here will be a Future completed with Failure(ArithmeticException)

  println(Await.result(result, 1.second))


  Await.ready(system.terminate(), 3.seconds)

  println("-----\n")
}
