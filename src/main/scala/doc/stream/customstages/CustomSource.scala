package doc.stream.customstages

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl._

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}


object CustomSource extends App {

  implicit val system: ActorSystem = ActorSystem("CustomSource")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContext = system.dispatcher

  println("\n-----")


  //#custom-source-example
  import akka.stream.Attributes
  import akka.stream.Outlet
  import akka.stream.SourceShape
  import akka.stream.stage.GraphStage
  import akka.stream.stage.GraphStageLogic
  import akka.stream.stage.OutHandler

  class NumbersSource extends GraphStage[SourceShape[Int]] {

    val out: Outlet[Int] = Outlet("NumbersSource")
    override val shape: SourceShape[Int] = SourceShape(out)

    override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
      new GraphStageLogic(shape) {
        // All state MUST be inside the GraphStageLogic,
        // never inside the enclosing GraphStage.
        // This state is safe to access and modify from all the
        // callbacks that are provided by GraphStageLogic and the
        // registered handlers.
        private var counter = 1

        setHandler(out, new OutHandler {
          override def onPull(): Unit = {
            push(out, counter)
            counter += 1
          }
        })
      }
  }

  // A GraphStage is a proper Graph, just like what GraphDSL.create would return
  val sourceGraph: Graph[SourceShape[Int], NotUsed] = new NumbersSource

  // Create a Source from the Graph to access the DSL
  val mySource: Source[Int, NotUsed] = Source.fromGraph(sourceGraph)

  // Returns 55
  val result1: Future[Int] = mySource.take(10).runFold(0)(_ + _)

  // The source is reusable. This returns 5050
  val result2: Future[Int] = mySource.take(100).runFold(0)(_ + _)
  //#simple-source-usage

  println(Await.result(result1, 3.seconds)) // should ===(55)
  println(Await.result(result2, 3.seconds)) // should ===(5050)


  Await.result(system.terminate(), 3.seconds)

  println("-----\n")
}
