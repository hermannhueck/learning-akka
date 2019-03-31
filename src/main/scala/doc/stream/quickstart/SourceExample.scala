package doc.stream.quickstart

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import akka.{Done, NotUsed}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}


object SourceExample extends App {

  implicit val system: ActorSystem = ActorSystem("reactive-tweets")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val source: Source[Int, NotUsed] = Source(1 to 100)

  println("\n-----")

  val result: Future[Done] =
    source.runForeach(i ⇒ println(i))

  Await.result(result, 3.seconds)
  Await.result(system.terminate(), 3.seconds)

  println("-----\n")
}
