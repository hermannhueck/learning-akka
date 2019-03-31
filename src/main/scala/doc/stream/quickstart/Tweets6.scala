package doc.stream.quickstart

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}


object Tweets6 extends App {

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
  implicit val ec: ExecutionContext = system.dispatcher

  println("\n-----")

  val count: Flow[Tweet, Int, NotUsed] = Flow[Tweet].map(_ ⇒ 1)

  val sumSink: Sink[Int, Future[Int]] = Sink.fold[Int, Int](0)(_ + _)

  val counterGraph: RunnableGraph[Future[Int]] =
    tweets
      .via(count)
      .toMat(sumSink)(Keep.right)

  val sum: Future[Int] = counterGraph.run()

  sum.foreach(count ⇒ println(s"Total tweets processed: $count"))

  Await.result(system.terminate(), 3.seconds)

  println("-----\n")

  def slowComputation(t: Tweet): Long = {
    Thread.sleep(100) // act as if performing some heavy computation
    42
  }
}
