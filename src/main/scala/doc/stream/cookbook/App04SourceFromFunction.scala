package doc.stream.cookbook

import java.util.UUID

import akka.NotUsed
import akka.stream.scaladsl.{Sink, Source}

import scala.collection.immutable
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

object App04SourceFromFunction extends AbstractApp {

  def builderFunction(): String = UUID.randomUUID.toString

  val source: Source[String, NotUsed] = Source.repeat(NotUsed).map(_ ⇒ builderFunction())
  //#source-from-function
  val result: Future[immutable.Seq[String]] = source.take(5).runWith(Sink.seq)

  Await.result(result, 1.second) foreach println


  Await.ready(system.terminate(), 3.seconds)
  println("-----\n")
}
