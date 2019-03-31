package doc.stream.cookbook

import akka.stream.scaladsl.{Sink, Source}

import scala.concurrent.Await
import scala.concurrent.duration._

object App01PrintlnDebug extends AbstractApp {

  val mySource = Source(List("1", "2", "3"))

  val loggedSource = mySource.map { elem =>
    println(elem); elem
  }

  val result = loggedSource.runWith(Sink.ignore)

  Await.ready(result, 1.second)


  Await.ready(system.terminate(), 3.seconds)
  println("-----\n")
}
