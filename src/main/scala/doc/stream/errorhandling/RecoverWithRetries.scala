package doc.stream.errorhandling

import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl._

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}


object RecoverWithRetries extends App {

  implicit val system: ActorSystem = ActorSystem("RecoverWithRetries")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContext = system.dispatcher

  println("\n-----")


  //#recoverWithRetries
  val planB = Source(List("five", "six", "seven", "eight"))

  val result = Source(0 to 10).map(n ⇒
    if (n < 5) n.toString
    else throw new RuntimeException("Boom!")
  ).recoverWithRetries(attempts = 1, {
    case _: RuntimeException ⇒ planB
  }).runForeach(println)

  Await.ready(result, 1.second)


  Await.ready(system.terminate(), 3.seconds)

  println("-----\n")
}
