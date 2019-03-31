package doc.stream.quickstart

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ClosedShape, Graph}
import akka.stream.scaladsl._
import akka.{Done, NotUsed}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}


object Tweets4 extends App {

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

  val hashtags: Source[Hashtag, NotUsed] =
    tweets
      .mapConcat(_.hashtags.toList)

  val writeAuthors: Sink[Author, Future[Done]] = Sink foreach println
  val writeHashtags: Sink[Hashtag, Future[Done]] = Sink foreach println

  val graph: Graph[ClosedShape, NotUsed] = GraphDSL.create() { implicit b =>

    import GraphDSL.Implicits._

    val bcast = b.add(Broadcast[Tweet](2))
    tweets ~> bcast.in
    bcast.out(0) ~> Flow[Tweet].map(_.author) ~> writeAuthors
    bcast.out(1) ~> Flow[Tweet].mapConcat(_.hashtags.toList) ~> writeHashtags
    ClosedShape
  }

  println("\n-----")

  val rg: RunnableGraph[NotUsed] = RunnableGraph.fromGraph(graph)
  rg.run()

  Await.result(system.terminate(), 3.seconds)

  println("-----\n")
}
