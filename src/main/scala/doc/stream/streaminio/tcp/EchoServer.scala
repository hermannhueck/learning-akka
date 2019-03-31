package doc.stream.streaminio.tcp

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl.Tcp.{IncomingConnection, ServerBinding}
import akka.stream.scaladsl._
import akka.testkit.SocketUtil
import akka.util.ByteString

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}


object EchoServer extends App {

  implicit val system: ActorSystem = ActorSystem("LogError")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContext = system.dispatcher

  println("\n-----")


  //val (host, port) = SocketUtil.temporaryServerHostnameAndPort()
  val (host, port) = ("localhost", 8888)

  import akka.stream.scaladsl.Framing

  val connections: Source[IncomingConnection, Future[ServerBinding]] =
    Tcp().bind(host, port)
  println(s"EchoServer accepting connections at: host = $host, port = $port")

  connections runForeach { connection ⇒

    println(s"New connection from: ${connection.remoteAddress}")

    val frameToLines: Flow[ByteString, ByteString, NotUsed] =
      Framing.delimiter(ByteString("\n"), maximumFrameLength = 256, allowTruncation = true)

    val echo = Flow[ByteString]
      .via(frameToLines)
      .map(_.utf8String)
      .map(_ + " !!!\n")
      .map(ByteString(_))

    connection.handleWith(echo)
  }
}
