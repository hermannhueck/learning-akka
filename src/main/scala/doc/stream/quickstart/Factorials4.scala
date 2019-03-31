package doc.stream.quickstart

import java.nio.file.Paths

import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.stream.scaladsl._
import akka.stream.{ActorMaterializer, IOResult}
import akka.util.ByteString

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}


object Factorials4 extends App {

  implicit val system: ActorSystem = ActorSystem("reactive-tweets")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val source: Source[Int, NotUsed] = Source(1 to 100)

  val factorials: Source[BigInt, NotUsed] = source.scan(BigInt(1))((acc, next) ⇒ acc * next)

  println("\n-----")

  val result: Future[Done] =
    factorials
      .zipWith(Source(0 to 20))((num, idx) ⇒ s"$idx! = $num")
      .throttle(1, 1.second)
      .runForeach(println)

  println(Await.result(result, 21.seconds))
  Await.result(system.terminate(), 3.seconds)

  println("-----\n")
}
