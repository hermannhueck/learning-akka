package doc.stream.errorhandling

import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl._

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}


object SupervisionResumeForFlow extends App {

  val decider: Supervision.Decider = {
    case _: ArithmeticException ⇒ Supervision.Resume
    case _                      ⇒ Supervision.Stop
  }

  implicit val system: ActorSystem = ActorSystem("SupervisionResumeForFlow")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContext = system.dispatcher

  println("\n-----")


  val flow = Flow[Int]
    .filter(100 / _ < 50).map(elem ⇒ 100 / (5 - elem))
    .withAttributes(ActorAttributes.supervisionStrategy(decider))
  val source = Source(0 to 5).via(flow)

  val result = source.runWith(Sink.fold(0)(_ + _))
  // the elements causing division by zero will be dropped
  // result here will be a Future completed with Success(150)

  println(Await.result(result, 1.second)) // should be(150)


  Await.ready(system.terminate(), 3.seconds)

  println("-----\n")
}
