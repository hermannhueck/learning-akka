package doc.stream.customstages

import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl._

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}


object ExtendingSource extends App {

  implicit val system: ActorSystem = ActorSystem("ExtendingSource")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContext = system.dispatcher

  println("\n-----")


  //#extending-source
  implicit class SourceDuplicator[Out, Mat](s: Source[Out, Mat]) {
    def duplicateElements: Source[Out, Mat] = s.via(new OneToMany2.Duplicator)
  }

  val s = Source(1 to 3).duplicateElements

  val result = s.runWith(Sink.seq)
  println(Await.result(result, 3.seconds)) //  should ===(Seq(1, 1, 2, 2, 3, 3))


  Await.result(system.terminate(), 3.seconds)

  println("-----\n")
}
