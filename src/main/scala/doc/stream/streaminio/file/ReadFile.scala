package doc.stream.streaminio.file

import java.nio.file.{Path, Paths}

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.{FileIO, Flow, Framing, Keep, Sink, Source}
import akka.stream.{ActorMaterializer, IOResult}
import akka.util.ByteString

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

object ReadFile extends App {

  implicit val system: ActorSystem = ActorSystem("ReadFile")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContext = system.dispatcher

  println("\n-----")


  val filename = "README.md"

  val file: Path = Paths.get(filename)

  private val fileSource: Source[ByteString, Future[IOResult]] = FileIO.fromPath(file)

  val frameToLines: Flow[ByteString, String, NotUsed] =
    Framing.delimiter(ByteString("\n"), maximumFrameLength = 256, allowTruncation = true)
    .map(_.utf8String)

  val (ioresult: Future[IOResult], lines: Future[Seq[String]]) =
    fileSource
      .via(frameToLines)
      .toMat(Sink.seq)(Keep.both)
      .run()

  Await.result(lines, 1.second) foreach println
  println("-----")
  println(Await.result(ioresult, 1.second))


  Await.ready(system.terminate(), 3.seconds)

  println("-----\n")
}
