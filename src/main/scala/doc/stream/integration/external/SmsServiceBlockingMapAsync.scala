package doc.stream.integration.external

import akka.NotUsed
import akka.actor.{ActorRef, ActorSystem}
import akka.stream._
import akka.stream.scaladsl._
import akka.testkit.TestProbe
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}


object SmsServiceBlockingMapAsync extends App {

  val config = ConfigFactory.parseString("""
    #//#blocking-dispatcher-config
    blocking-dispatcher {
      executor = "thread-pool-executor"
      thread-pool-executor {
        core-pool-size-min    = 10
        core-pool-size-max    = 10
      }
    }
    #//#blocking-dispatcher-config

    akka.actor.default-mailbox.mailbox-type = akka.dispatch.UnboundedMailbox
    """)

  implicit val system: ActorSystem = ActorSystem("SmsServiceBlockingMapAsync", config)
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


  class SmsServer(probe: ActorRef) {
    def send(text: TextMessage): Unit = {
      probe ! text.to
    }
  }

  val probe = TestProbe()
  val addressSystem = new AddressSystem
  val smsServer = new SmsServer(probe.ref)

  val authors = tweets.filter(_.hashtags.contains(akkaTag)).map(_.author)

  val phoneNumbers =
    authors.mapAsync(4)(author => addressSystem.lookupPhoneNumber(author.handle)).collect {
      case Some(phoneNo) => phoneNo
    }

  val blockingExecutionContext = system.dispatchers.lookup("blocking-dispatcher")

  val sendTextMessages: RunnableGraph[NotUsed] =
    phoneNumbers
      .mapAsync(4)(phoneNo => {
        Future {
          smsServer.send(TextMessage(to = phoneNo, body = "I like your tweet"))
        }(blockingExecutionContext)
      })
      .to(Sink.ignore)

  sendTextMessages.run()

  assert(probe.receiveN(7).toSet ==
    Set(
      "rolandkuhn".hashCode.toString,
      "patriknw".hashCode.toString,
      "bantonsson".hashCode.toString,
      "drewhk".hashCode.toString,
      "ktosopl".hashCode.toString,
      "mmartynas".hashCode.toString,
      "akkateam".hashCode.toString))


  Await.result(system.terminate(), 3.seconds)

  println("-----\n")
}
