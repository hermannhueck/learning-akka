package doc.stream.modularity

import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl._

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}


object EmbeddedClosedShape extends App {

  implicit val system: ActorSystem = ActorSystem("SimpleGraph")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContext = system.dispatcher

  println("\n-----")


  //#embed-closed
  val closed1 = Source.single(0).to(Sink.foreach(println))
  closed1.run()

  val closed2 = RunnableGraph.fromGraph(GraphDSL.create() { implicit builder ⇒
    val embeddedClosed: ClosedShape = builder.add(closed1)
    // …
    embeddedClosed
  })
  closed2.run()


  //Await.result(result, 3.seconds)
  Await.result(system.terminate(), 3.seconds)

  println("-----\n")
}
