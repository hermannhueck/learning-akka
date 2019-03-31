package doc.stream.cookbook

import akka.stream.scaladsl.{Sink, Source}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

object App06StreamToSeq extends AbstractApp {

  val mySource = Source(1 to 3).map(_.toString)

  // Dangerous: might produce a collection with 2 billion elements!
  val unbounded: Future[Seq[String]] = mySource.runWith(Sink.seq)
  println(Await.result(unbounded, 1.seconds))

  val MAX_ALLOWED_SIZE = 100

  // OK. Future will fail with a `StreamLimitReachedException`
  // if the number of incoming elements is larger than max
  val limited: Future[Seq[String]] =
  mySource.limit(MAX_ALLOWED_SIZE).runWith(Sink.seq)
  println(Await.result(limited, 1.seconds))

  // OK. Collect up until max-th elements only, then cancel upstream
  val ignoreOverflow: Future[Seq[String]] =
    mySource.take(MAX_ALLOWED_SIZE).runWith(Sink.seq)
  println(Await.result(ignoreOverflow, 1.seconds))


  Await.ready(system.terminate(), 3.seconds)
  println("-----\n")
}
