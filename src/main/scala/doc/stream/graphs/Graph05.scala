package doc.stream.graphs

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl._

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}


object Graph05 extends App {

  implicit val system: ActorSystem = ActorSystem("SimpleGraph")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContext = system.dispatcher

  println("\n-----")


  //#source-from-partial-graph-dsl
  val pairs = Source.fromGraph(GraphDSL.create() { implicit builder ⇒

    import GraphDSL.Implicits._

    // prepare graph elements
    val zip = builder.add(Zip[Int, Int]())
    def ints: Source[Int, NotUsed] = Source.fromIterator(() ⇒ Iterator.from(1))

    // connect the graph
    ints.filter(_ % 2 != 0) ~> zip.in0
    ints.filter(_ % 2 == 0) ~> zip.in1

    // expose port
    SourceShape(zip.out)
  })

  val firstPair: Future[(Int, Int)] = pairs.runWith(Sink.head)

  println(Await.result(firstPair, 300.millis)) // should equal(1 -> 2)



  //Await.result(result, 3.seconds)
  Await.result(system.terminate(), 3.seconds)

  println("-----\n")
}
