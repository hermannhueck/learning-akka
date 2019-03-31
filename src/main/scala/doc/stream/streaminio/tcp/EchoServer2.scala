package doc.stream.streaminio.tcp

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl.Tcp.{IncomingConnection, ServerBinding}
import akka.stream.scaladsl._
import akka.util.ByteString

import scala.concurrent.{ExecutionContext, Future}


object EchoServer2 extends App {

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

  connections.to(Sink.foreach { connection ⇒

    println(s"New connection from: ${connection.remoteAddress}")

    // server logic, parses incoming commands
    val commandParser = Flow[String].takeWhile(_ != "BYE").map(_ + "!")

    import connection._

    val welcomeMsg = s"Welcome to: $localAddress, you are: $remoteAddress!"
    val welcome = Source.single(welcomeMsg)

    val frameToLines: Flow[ByteString, ByteString, NotUsed] =
      Framing.delimiter(ByteString("\n"), maximumFrameLength = 256, allowTruncation = true)

    val serverLogic = Flow[ByteString]
      .via(frameToLines)
      .map(_.utf8String)
      //.map { command ⇒ serverProbe.ref ! command; command }
      .map { command ⇒ println(command); command }
      .via(commandParser)
      // merge in the initial banner after parser
      .merge(welcome)
      .map(_ + "\n")
      .map(ByteString(_))

    connection.handleWith(serverLogic)
  }).run()
}
