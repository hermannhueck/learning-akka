package doc.stream.customstages

import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl._

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}


object ExtendingFlow extends App {

  implicit val system: ActorSystem = ActorSystem("ExtendingFlow")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContext = system.dispatcher

  println("\n-----")


  //#extending-flow
  implicit class FlowDuplicator[In, Out, Mat](s: Flow[In, Out, Mat]) {
    def duplicateElements: Flow[In, Out, Mat] = s.via(new OneToMany2.Duplicator)
  }

  val f = Flow[Int].duplicateElements

  val result = Source(1 to 3).via(f).runWith(Sink.seq)
  println(Await.result(result, 3.seconds)) //  should ===(Seq(1, 1, 2, 2, 3, 3))


  Await.result(system.terminate(), 3.seconds)

  println("-----\n")
}
