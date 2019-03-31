package doc.stream.errorhandling

import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl._

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}


object Recover extends App {

  implicit val system: ActorSystem = ActorSystem("Recover")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContext = system.dispatcher

  println("\n-----")


  //#recover
  val result = Source(0 to 6).map(n ⇒
    if (n < 5) n.toString
    else throw new RuntimeException("Boom!")
  ).recover {
    case _: RuntimeException ⇒ "stream truncated"
  }.runForeach(println)

  Await.ready(result, 1.second)


  Await.ready(system.terminate(), 3.seconds)

  println("-----\n")
}
