package doc.stream.graphs

import akka.Done
import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.stream._
import akka.stream.scaladsl._

import scala.collection.immutable
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}


object Graph08 extends App {

  implicit val system: ActorSystem = ActorSystem("SimpleGraph")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContext = system.dispatcher

  println("\n-----")


  class MyActor extends Actor {
    override def receive: Receive = {
      case msg => println(msg)
    }
  }
  val actorRef: ActorRef = system.actorOf(Props[MyActor])

  //#sink-combine
  val sendRmotely: Sink[Int, Future[Done]] = Sink.foreach[Int](println)
  val localProcessing: Sink[Int, Future[Done]] = Sink.foreach[Int](println) // (_ ⇒ /* do something useful */ ())

  val sink = Sink.combine(sendRmotely, localProcessing)(Broadcast[Int](_))

  val result = Source(List(0, 1, 2)).runWith(sink)



  //Await.result(result, 3.seconds)
  Await.result(system.terminate(), 3.seconds)

  println("-----\n")
}
