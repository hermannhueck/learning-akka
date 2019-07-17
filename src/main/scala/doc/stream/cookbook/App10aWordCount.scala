package doc.stream.cookbook

import akka.NotUsed
import akka.stream.scaladsl.{Sink, Source}

import scala.concurrent.Await
import scala.concurrent.duration._

object App10aWordCount extends AkkaStreamApp {

  val MaximumDistinctWords = 1000

  def words = Source(List("hello", "world", "and", "hello", "universe", "akka") ++ List.fill(1000)("rocks!"))

  val countsSource: Source[(String, Int), NotUsed] = words
    // split the words into separate streams first
    .groupBy(MaximumDistinctWords, identity)
    //transform each element to pair with number of words in it
    .map(_ -> 1)
    // add counting logic to the streams
    .reduce((l, r) => (l._1, l._2 + r._2))
    // get a stream of word counts
    .mergeSubstreams

  val counts: Set[(String, Int)] = Await.result(countsSource.limit(10).runWith(Sink.seq), 3.seconds).toSet
  assert(counts == Set(("hello", 2), ("world", 1), ("and", 1), ("universe", 1), ("akka", 1), ("rocks!", 1000)))
  counts.toList sortWith { case (c1, c2) => c1._2 > c2._2 } foreach println


  Await.ready(system.terminate(), 3.seconds)
  println("-----\n")
}
