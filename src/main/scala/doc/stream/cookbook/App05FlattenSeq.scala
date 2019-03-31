package doc.stream.cookbook

import akka.NotUsed
import akka.stream.scaladsl.{Sink, Source}

import scala.concurrent.Await
import scala.concurrent.duration._

object App05FlattenSeq extends AbstractApp {

  type Message = String

  val someDataSource: Source[List[String], NotUsed] = Source(List(List("1"), List("2"), List("3", "4", "5"), List("6", "7")))

  val myData: Source[List[Message], NotUsed] = someDataSource
  val flattened: Source[Message, NotUsed] = myData.mapConcat(identity)

  println(Await.result(flattened.limit(8).runWith(Sink.seq), 3.seconds)) // should be(List("1", "2", "3", "4", "5", "6", "7"))


  Await.ready(system.terminate(), 3.seconds)
  println("-----\n")
}
