package doc.stream.streaminio.framing

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{JsonFraming, Source}
import akka.util.ByteString

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}

object FramingJson extends App {

  implicit val system: ActorSystem = ActorSystem("LogError")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContext = system.dispatcher

  println("\n-----")


  val input =
    """
      |[
      | { "name" : "john" },
      | { "name" : "Ég get etið gler án þess að meiða mig" },
      | { "name" : "jack" },
      |]
      |""".stripMargin // also should complete once notices end of array

  val resultFuture = Source.single(ByteString(input))
    .via(JsonFraming.objectScanner(Int.MaxValue))
    .runFold(Seq.empty[String]) {
      case (acc, entry) ⇒ acc ++ Seq(entry.utf8String)
    }

  val result = Await.result(resultFuture, 1.second)
  println(result)

  assert(result ==
    Seq(
      """{ "name" : "john" }""",
      """{ "name" : "Ég get etið gler án þess að meiða mig" }""",
      """{ "name" : "jack" }"""
    )
  )


  Await.ready(system.terminate(), 3.seconds)

  println("-----\n")
}
