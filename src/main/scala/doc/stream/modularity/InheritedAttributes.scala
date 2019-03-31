package doc.stream.modularity

import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl._

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}


object InheritedAttributes extends App {

  implicit val system: ActorSystem = ActorSystem("SimpleGraph")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContext = system.dispatcher

  println("\n-----")


  //#attributes-inheritance
  import Attributes._
  val nestedSource =
    Source.single(0)
      .map(_ + 1)
      .named("nestedSource") // Wrap, no inputBuffer set

  val nestedFlow =
    Flow[Int].filter(_ != 0)
      .via(Flow[Int].map(_ - 2).withAttributes(inputBuffer(4, 4))) // override
      .named("nestedFlow") // Wrap, no inputBuffer set

  val nestedSink =
    nestedFlow.to(Sink.fold(0)(_ + _)) // wire an atomic sink to the nestedFlow
      .withAttributes(name("nestedSink") and inputBuffer(3, 3)) // override


  //Await.result(result, 3.seconds)
  Await.result(system.terminate(), 3.seconds)

  println("-----\n")
}
