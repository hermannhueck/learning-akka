package doc.stream.materilization

import akka.Done
import akka.actor.ActorSystem
import akka.stream.scaladsl._
import akka.stream.{ActorMaterializer, OverflowStrategy}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}


object PreMaterialization extends App {

  implicit val system: ActorSystem = ActorSystem("PreMaterialization")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContext = system.dispatcher

  println("\n-----")


  val matValuePoweredSource =
    Source.actorRef[String](bufferSize = 100, overflowStrategy = OverflowStrategy.fail)

  val (actorRef, source) = matValuePoweredSource.preMaterialize()

  actorRef ! "Hello!"

  val result: Future[Done] = source.runWith(Sink.foreach(println))


  //Await.result(result, 3.seconds)
  Await.result(system.terminate(), 3.seconds)

  println("-----\n")
}
