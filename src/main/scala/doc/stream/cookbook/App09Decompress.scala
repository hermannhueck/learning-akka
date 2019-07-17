package doc.stream.cookbook

import akka.NotUsed
import akka.stream.scaladsl.{Sink, Source}
import akka.util.ByteString

import scala.concurrent.Await
import scala.concurrent.duration._

object App09Decompress extends AkkaStreamApp {

  import akka.stream.scaladsl.Compression

  val compressed =
    Source.single(ByteString.fromString("Hello World")).via(Compression.gzip)

  val uncompressedSource: Source[String, NotUsed] = compressed.via(Compression.gunzip()).map(_.utf8String)

  val uncompressed: String = Await.result(uncompressedSource.runWith(Sink.head), 3.seconds)
  assert(uncompressed == "Hello World")
  println(uncompressed)


  Await.ready(system.terminate(), 3.seconds)
  println("-----\n")
}
