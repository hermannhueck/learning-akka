package doc.stream.modularity

import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl._

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}


object Nested extends App {

  implicit val system: ActorSystem = ActorSystem("SimpleGraph")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContext = system.dispatcher

  println("\n-----")


  val nestedSource =
    Source.single(0) // An atomic source
      .map(_ + 1) // an atomic processing stage
      .named("nestedSource") // wraps up the current Source and gives it a name

  val nestedFlow =
    Flow[Int].filter(_ != 0) // an atomic processing stage
      .map(_ - 2) // another atomic processing stage
      .named("nestedFlow") // wraps up the Flow, and gives it a name

  val nestedSink =
    nestedFlow.to(Sink.fold(0)(_ + _)) // wire an atomic sink to the nestedFlow
      .named("nestedSink") // wrap it up

  //#reuse
  // Create a RunnableGraph from our components
  val runnableGraph = nestedSource.to(nestedSink)

  // Usage is uniform, no matter if modules are composite or atomic
  val runnableGraph2 = Source.single(0).to(Sink.fold(0)(_ + _))


  //Await.result(result, 3.seconds)
  Await.result(system.terminate(), 3.seconds)

  println("-----\n")
}
