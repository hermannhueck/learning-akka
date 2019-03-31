package doc.stream.quickstart

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.stream.scaladsl._
import akka.{Done, NotUsed}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}


object Tweets5 extends App {

  final case class Author(handle: String)

  final case class Hashtag(name: String)

  final case class Tweet(author: Author, timestamp: Long, body: String) {
    def hashtags: Set[Hashtag] = body.split(" ").collect {
      case t if t.startsWith("#") ⇒ Hashtag(t.replaceAll("[^#\\w]", ""))
    }.toSet
  }

  val akkaTag = Hashtag("#akka")

  val tweets: Source[Tweet, NotUsed] = Source(
    Tweet(Author("rolandkuhn"), System.currentTimeMillis, "#akka rocks!") ::
      Tweet(Author("patriknw"), System.currentTimeMillis, "#akka !") ::
      Tweet(Author("bantonsson"), System.currentTimeMillis, "#akka !") ::
      Tweet(Author("drewhk"), System.currentTimeMillis, "#akka !") ::
      Tweet(Author("ktosopl"), System.currentTimeMillis, "#akka on the rocks!") ::
      Tweet(Author("mmartynas"), System.currentTimeMillis, "wow #akka !") ::
      Tweet(Author("akkateam"), System.currentTimeMillis, "#akka rocks!") ::
      Tweet(Author("bananaman"), System.currentTimeMillis, "#bananas rock!") ::
      Tweet(Author("appleman"), System.currentTimeMillis, "#apples rock!") ::
      Tweet(Author("drama"), System.currentTimeMillis, "we compared #apples to #oranges!") ::
      Nil)

  implicit val system: ActorSystem = ActorSystem("reactive-tweets")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  println("\n-----")

  val result: Future[Done] =
    tweets
      .buffer(10, OverflowStrategy.dropHead)
      .map(slowComputation)
      .runWith(Sink.foreach(println))

  Await.result(result, 3.seconds)
  Await.result(system.terminate(), 3.seconds)

  println("-----\n")

  def slowComputation(t: Tweet): Long = {
    Thread.sleep(100) // act as if performing some heavy computation
    42
  }
}
