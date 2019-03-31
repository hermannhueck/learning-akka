package doc.stream.errorhandling

import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl._

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}


object SupervisionResumeGlobal extends App {

  val decider: Supervision.Decider = {
    case _: ArithmeticException ⇒ Supervision.Resume
    case _                      ⇒ Supervision.Stop
  }

  implicit val system: ActorSystem = ActorSystem("SupervisionResumeGlobal")
  implicit val materializer: ActorMaterializer = ActorMaterializer(ActorMaterializerSettings(system).withSupervisionStrategy(decider))
  implicit val ec: ExecutionContext = system.dispatcher

  println("\n-----")


  val source = Source(0 to 5).map(100 / _)
  val result = source.runWith(Sink.fold(0)(_ + _))
  // the element causing division by zero will be dropped
  // result here will be a Future completed with Success(228)

  println(Await.result(result, 1.second)) // should be(228)


  Await.ready(system.terminate(), 3.seconds)

  println("-----\n")
}
