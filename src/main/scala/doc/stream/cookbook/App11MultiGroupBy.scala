package doc.stream.cookbook

import akka.NotUsed
import akka.stream.scaladsl.{Sink, Source}

import scala.collection.immutable
import scala.concurrent.Await
import scala.concurrent.duration._

object App11MultiGroupBy extends AbstractApp {

  type Message = String

  case class Topic(name: String)

  val elems = Source(List("1: a", "1: b", "all: c", "all: d", "1: e"))

  val extractTopics: Message => List[Topic] =
    msg =>
      if (msg.startsWith("1"))
        List(Topic("1"))
      else
        List(Topic("1"), Topic("2"))


  val topicMapper: Message => immutable.Seq[Topic] =
    msg =>
      if (msg.startsWith("1"))
        List(Topic("1"))
      else
        List(Topic("1"), Topic("2"))

  val messageAndTopicOrg: Source[(Message, Topic), NotUsed] = elems.mapConcat { msg: Message =>
    val topicsForMessage = topicMapper(msg)
    // Create a (Msg, Topic) pair for each of the topics the message belongs to
    topicsForMessage.map(msg -> _)
  }

  val messageAndTopic: Source[(Message, Topic), NotUsed] = elems.mapConcat { msg: Message =>
    // Create a (Msg, Topic) pair for each of the topics the message belongs to
    topicMapper(msg).map(topic => msg -> topic)
  }

  val multiGroups = messageAndTopic.groupBy(2, _._2).map {
    case (msg, topic) =>
      // do what needs to be done
      (msg, topic)
  }

  val future = multiGroups
    .grouped(10)
    .mergeSubstreams
    .map(g => g.head._2.name + g.map(_._1).mkString("[", ", ", "]"))
    .limit(10)
    .runWith(Sink.seq)

  private val result: Set[Message] = Await.result(future, 3.seconds).toSet
  assert(result == Set("1[1: a, 1: b, all: c, all: d, 1: e]", "2[all: c, all: d]"))
  result foreach println


  Await.ready(system.terminate(), 3.seconds)
  println("-----\n")
}
