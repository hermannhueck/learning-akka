package doc.stream.cookbook

import akka.NotUsed
import akka.stream.scaladsl.{Flow, Sink, Source}

import scala.concurrent.Await
import scala.concurrent.duration._

object App10bReduceByKey extends AbstractApp {

  def reduceByKey[In, K, Out](maximumGroupSize: Int,
                              groupKey: In => K,
                              map: In => Out)
                             (reduce: (Out, Out) => Out): Flow[In, (K, Out), NotUsed] =
    Flow[In]
      .groupBy[K](maximumGroupSize, groupKey)
      .map(e => groupKey(e) -> map(e))
      .reduce((l, r) => l._1 -> reduce(l._2, r._2))
      .mergeSubstreams

  val MaximumDistinctWords = 1000

  def words = Source(List("hello", "world", "and", "hello", "universe", "akka") ++ List.fill(1000)("rocks!"))

  val countsSource =
    words via reduceByKey(
      MaximumDistinctWords,
      groupKey = (word: String) => word,
      map = (word: String) => 1
    )((left: Int, right: Int) => left + right)

  val counts: Set[(String, Int)] = Await.result(countsSource.limit(10).runWith(Sink.seq), 3.seconds).toSet
  assert(counts == Set(("hello", 2), ("world", 1), ("and", 1), ("universe", 1), ("akka", 1), ("rocks!", 1000)))
  counts.toList sortWith { case (c1, c2) => c1._2 > c2._2 } foreach println


  Await.ready(system.terminate(), 3.seconds)
  println("-----\n")
}
