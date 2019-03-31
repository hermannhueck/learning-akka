package doc.stream.customstages

import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl._

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}


object GraphOperatorChain extends App {

  implicit val system: ActorSystem = ActorSystem("GraphOperatorChain")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContext = system.dispatcher

  println("\n-----")


  val sink = Sink.fold[List[Int], Int](List.empty[Int])((acc, n) ⇒ acc :+ n)

  //#graph-operator-chain
  val resultFuture = Source(1 to 5)
    .via(new ManyToOne.Filter(_ % 2 == 0))
    .via(new OneToMany2.Duplicator())
    .via(new OneToOne.Map(_ / 2))
    .runWith(sink)

  println(Await.result(resultFuture, 3.seconds)) // should ===(List(1, 1, 2, 2))


  Await.result(system.terminate(), 3.seconds)

  println("-----\n")
}
