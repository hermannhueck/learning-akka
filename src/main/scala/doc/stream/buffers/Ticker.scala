package doc.stream.buffers

import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl._

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}


object Ticker extends App {

  implicit val system: ActorSystem = ActorSystem("SimpleGraph")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContext = system.dispatcher

  println("\n-----")


  //#buffering-abstraction-leak
  import scala.concurrent.duration._
  case class Tick()

  val g = RunnableGraph.fromGraph(GraphDSL.create() { implicit b ⇒
    import GraphDSL.Implicits._

    // this is the asynchronous stage in this graph
    val zipper = b.add(ZipWith[Tick, Int, Int]((tick, count) ⇒ count).async)

    Source.tick(initialDelay = 3.second, interval = 3.second, Tick()) ~> zipper.in0

    Source.tick(initialDelay = 1.second, interval = 1.second, "message!")
      .conflateWithSeed(seed = (_) ⇒ 1)((count, _) ⇒ count + 1) ~> zipper.in1

    zipper.out ~> Sink.foreach(println)
    ClosedShape
  })

  g.run()


  Await.result(system.terminate(), 3.seconds)

  println("-----\n")
}
