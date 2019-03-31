package doc.stream.integration.external

import akka.NotUsed
import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.stream._
import akka.stream.scaladsl._
import akka.testkit.TestProbe
import akka.util.Timeout

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}


object EmailServiceMapAsync extends App {

  implicit val system: ActorSystem = ActorSystem("EmailServiceMapAsync")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContext = system.dispatcher

  println("\n-----")


  final case class Author(handle: String)

  final case class Hashtag(name: String)

  final case class Tweet(author: Author, timestamp: Long, body: String) {
    def hashtags: Set[Hashtag] =
      body
        .split(" ")
        .collect {
          case t if t.startsWith("#") => Hashtag(t.replaceAll("[^#\\w]", ""))
        }
        .toSet
  }

  val akkaTag = Hashtag("#akka")

  abstract class TweetSourceDecl {
    val tweets: Source[Tweet, NotUsed]
  }

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

  class AddressSystem {
    def lookupEmail(handle: String): Future[Option[String]] =
      Future.successful(Some(handle + "@somewhere.com"))

    def lookupPhoneNumber(handle: String): Future[Option[String]] =
      Future.successful(Some(handle.hashCode.toString))
  }

  class AddressSystem2 {
    def lookupEmail(handle: String): Future[String] =
      Future.successful(handle + "@somewhere.com")
  }

  final case class Email(to: String, title: String, body: String)
  final case class TextMessage(to: String, body: String)

  class EmailServer(probe: ActorRef) {
    def send(email: Email): Future[Unit] = {
      // ...
      probe ! email.to
      Future.successful(())
    }
  }

  val probe = TestProbe()
  val addressSystem = new AddressSystem
  val emailServer = new EmailServer(probe.ref)

  val authors: Source[Author, NotUsed] =
    tweets.filter(_.hashtags.contains(akkaTag)).map(_.author)

  val emailAddresses: Source[String, NotUsed] =
    authors.mapAsync(4)(author => addressSystem.lookupEmail(author.handle)).collect {
      case Some(emailAddress) => emailAddress
    }

  val sendEmails: RunnableGraph[NotUsed] =
    emailAddresses
      .mapAsync(4)(address => {
        emailServer.send(Email(to = address, title = "Akka", body = "I like your tweet"))
      })
      .to(Sink.ignore)

  sendEmails.run()

  probe.expectMsg("rolandkuhn@somewhere.com")
  probe.expectMsg("patriknw@somewhere.com")
  probe.expectMsg("bantonsson@somewhere.com")
  probe.expectMsg("drewhk@somewhere.com")
  probe.expectMsg("ktosopl@somewhere.com")
  probe.expectMsg("mmartynas@somewhere.com")
  probe.expectMsg("akkateam@somewhere.com")


  Await.result(system.terminate(), 3.seconds)

  println("-----\n")
}
