package doc.stream.graphs

import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl._

import scala.collection.immutable
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}


object Graph03 extends App {

  implicit val system: ActorSystem = ActorSystem("SimpleGraph")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContext = system.dispatcher

  println("\n-----")


  //#graph-from-list
  val sinks = immutable.Seq("a", "b", "c").map(prefix ⇒
    Flow[String].filter(str ⇒ str.startsWith(prefix)).toMat(Sink.head[String])(Keep.right)
  )

  val g: RunnableGraph[Seq[Future[String]]] =
    RunnableGraph.fromGraph(GraphDSL.create(sinks) {
      implicit builder ⇒
        sinkList ⇒

          import GraphDSL.Implicits._

          val broadcast = builder.add(Broadcast[String](sinkList.size))

          Source(List("ax", "bx", "cx")) ~> broadcast
          sinkList.foreach(sink ⇒ broadcast ~> sink)

          ClosedShape
    })

  val matList: Seq[Future[String]] = g.run()

  val result: Seq[String] = Await.result(Future.sequence(matList), 3.seconds)

  println(result.size) // shouldBe 3
  println(result.head) // shouldBe "ax"
  println(result(1)) // shouldBe "bx"
  println(result(2)) // shouldBe "cx"


  //Await.result(result, 3.seconds)
  Await.result(system.terminate(), 3.seconds)

  println("-----\n")
}
