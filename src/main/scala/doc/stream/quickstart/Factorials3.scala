package doc.stream.quickstart

import java.nio.file.Paths

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl._
import akka.stream.{ActorMaterializer, IOResult}
import akka.util.ByteString

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}


object Factorials3 extends App {

  implicit val system: ActorSystem = ActorSystem("Factorials3")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val source: Source[Int, NotUsed] = Source(1 to 100)

  val factorials: Source[BigInt, NotUsed] = source.scan(BigInt(1))((acc, next) ⇒ acc * next)

  println("\n-----")

  val result: Future[IOResult] =
    factorials
      .map(_.toString)
      .runWith(lineSink("output/factorials.txt"))

  println(Await.result(result, 3.seconds))
  Await.result(system.terminate(), 3.seconds)

  println("-----\n")

  def lineSink(filename: String): Sink[String, Future[IOResult]] =
    Flow[String]
      .map(s ⇒ ByteString(s + "\n"))
      .toMat(FileIO.toPath(Paths.get(filename)))(Keep.right)
}
