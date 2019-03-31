package doc.stream.errorhandling

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.sse.ServerSentEvent
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream._
import akka.stream.scaladsl._

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future, Promise}


object RestartWithBackoffSource extends App {

  implicit val system: ActorSystem = ActorSystem("RestartWithBackoffSource")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContext = system.dispatcher

  println("\n-----")


  // Mock akka-http interfaces
  object Http {
    def apply(): Http.type = this
    def singleRequest(req: HttpRequest): Future[Unit] = Future.successful(())
  }
  case class HttpRequest(uri: String)
  case class Unmarshal(b: Any) {
    def to[T]: Future[T] = Promise[T]().future
  }
  case class ServerSentEvent()

  def doSomethingElse(): Unit = Thread sleep 30000L


  //#restart-with-backoff-source
  val restartSource = RestartSource.withBackoff(
    minBackoff = 3.seconds,
    maxBackoff = 30.seconds,
    randomFactor = 0.2, // adds 20% "noise" to vary the intervals slightly
    maxRestarts = 20 // limits the amount of restarts to 20
  ) { () ⇒
    // Create a source from a future of a source
    Source.fromFutureSource {
      // Make a single request with akka-http
      Http().singleRequest(HttpRequest(
        uri = "http://example.com/eventstream"
      ))
        // Unmarshall it as a source of server sent events
        .flatMap(Unmarshal(_).to[Source[ServerSentEvent, NotUsed]])
    }
  }

  val killSwitch = restartSource
    .viaMat(KillSwitches.single)(Keep.right)
    .toMat(Sink.foreach(event ⇒ println(s"Got event: $event")))(Keep.left)
    .run()

  doSomethingElse()

  killSwitch.shutdown()


  Await.ready(system.terminate(), 3.seconds)

  println("-----\n")
}
