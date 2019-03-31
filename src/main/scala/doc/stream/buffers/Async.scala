package doc.stream.buffers

import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl._

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}


object Async extends App {

  implicit val system: ActorSystem = ActorSystem("SimpleGraph")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContext = system.dispatcher

  println("\n-----")


  val result = Source(1 to 3)
    .map { i ⇒ println(s"A: $i"); i }.async
    .map { i ⇒ println(s"B: $i"); i }.async
    .map { i ⇒ println(s"C: $i"); i }.async
    .runWith(Sink.ignore)

  Await.result(result, 3.seconds)


  Await.result(system.terminate(), 3.seconds)

  println("-----\n")
}
