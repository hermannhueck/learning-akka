package doc.stream.graphs

import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl._

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}


object Graph07 extends App {

  implicit val system: ActorSystem = ActorSystem("SimpleGraph")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContext = system.dispatcher

  println("\n-----")


  //#source-combine
  val sourceOne = Source(List(1))
  val sourceTwo = Source(List(2))
  val merged = Source.combine(sourceOne, sourceTwo)(Merge(_))

  val mergedResult: Future[Int] = merged.runWith(Sink.fold(0)(_ + _))

  println(Await.result(mergedResult, 300.millis)) // should equal(3)



  //Await.result(result, 3.seconds)
  Await.result(system.terminate(), 3.seconds)

  println("-----\n")
}
