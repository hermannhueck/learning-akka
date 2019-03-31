package doc.stream.integration.actors

import akka.NotUsed
import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.stream._
import akka.stream.scaladsl._
import akka.util.Timeout

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}


object AskActor extends App {

  implicit val system: ActorSystem = ActorSystem("AskActor")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContext = system.dispatcher

  println("\n-----")


  //#ask-actor
  class Translator extends Actor {
    def receive: Receive = {
      case word: String ⇒
        // ... process message
        val reply = word.toUpperCase
        sender() ! reply // reply to the ask
    }
  }

  val ref: ActorRef = system.actorOf(Props[Translator])

  implicit val askTimeout: Timeout = Timeout(5.seconds)

  val words: Source[String, NotUsed] =
    Source(List("hello", "hi"))

  val result = words
    .ask[String](parallelism = 5)(ref)
    // continue processing of the replies from the actor
    .map(_.toList.flatMap(List(_, ' ')).mkString.trim) // intersperse
    .runWith(Sink foreach println)

  Await.ready(result, 1.second)


  Await.result(system.terminate(), 3.seconds)

  println("-----\n")
}
