package doc.stream.errorhandling

import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl._

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}


object SupervisionRestartForFlow extends App {

  val decider: Supervision.Decider = {
    case _: IllegalArgumentException ⇒ Supervision.Restart
    case _                           ⇒ Supervision.Stop
  }

  implicit val system: ActorSystem = ActorSystem("SupervisionRestartForFlow")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContext = system.dispatcher

  println("\n-----")


  val flow = Flow[Int]
    .scan(0) { (acc, elem) ⇒
      if (elem < 0) throw new IllegalArgumentException("negative not allowed")
      else acc + elem
    }
    .withAttributes(ActorAttributes.supervisionStrategy(decider))
  val source = Source(List(1, 3, -1, 5, 7)).via(flow)
  val result = source.limit(1000).runWith(Sink.seq)
  // the negative element cause the scan stage to be restarted,
  // i.e. start from 0 again
  // result here will be a Future completed with Success(Vector(0, 1, 4, 0, 5, 12))

  println(Await.result(result, 1.second)) // should be(Vector(0, 1, 4, 0, 5, 12))


  Await.ready(system.terminate(), 3.seconds)

  println("-----\n")
}
