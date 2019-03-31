package doc.stream.streaminio.tcp

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl._
import akka.util.ByteString

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.io.StdIn


object ReplClient extends App {

  implicit val system: ActorSystem = ActorSystem("LogError")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContext = system.dispatcher

  println("\n-----")


  val connection = Tcp().outgoingConnection("127.0.0.1", 8888)

  val replParser =
    Flow[String].takeWhile(_ != "q")
      .concat(Source.single("BYE"))
      .map { elem =>
        if (elem == "BYE")
          system.scheduler.scheduleOnce(300.milliseconds){system.terminate(); println("\nTERMINATED\n-----\n")} // tear down actor system
        elem
      }
      .map(elem ⇒ ByteString(s"$elem\n"))

  val frameToLines: Flow[ByteString, ByteString, NotUsed] =
    Framing.delimiter(ByteString("\n"), maximumFrameLength = 256, allowTruncation = true)

  val repl = Flow[ByteString]
    .via(frameToLines)
    .map(_.utf8String)
    .map(text ⇒ println("Server: " + text))
    .map(_ ⇒ StdIn.readLine("> "))
    .via(replParser)

  val connected: Future[Tcp.OutgoingConnection] = connection.join(repl).run()
}
