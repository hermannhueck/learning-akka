package doc.stream.cookbook

import akka.NotUsed
import akka.stream.FlowShape
import akka.stream.scaladsl.{Balance, Flow, GraphDSL, Merge, Sink, Source}

import scala.concurrent.Await
import scala.concurrent.duration._

object App13WorkerPool extends AbstractApp {

  val list = (1 to 10).toList
  val myJobs = Source(list)
  type Result = String

  def worker[IN]: Flow[IN, String, NotUsed] = Flow[IN].map(_ + " done")

  //#worker-pool
  def balancer[In, Out](worker: Flow[In, Out, Any], workerCount: Int): Flow[In, Out, NotUsed] = {
    import GraphDSL.Implicits._

    Flow.fromGraph(GraphDSL.create() { implicit b =>
      val balancer = b.add(Balance[In](workerCount, waitForAllDownstreams = true))
      val merge = b.add(Merge[Out](workerCount))

      for (_ <- 1 to workerCount) {
        // for each worker, add an edge from the balancer to the worker, then wire
        // it to the merge element
        balancer ~> worker.async ~> merge
      }

      FlowShape(balancer.in, merge.out)
    })
  }

  val processedJobs: Source[Result, NotUsed] = myJobs.via(balancer(worker, 3))
  //#worker-pool

  private val result: Set[Result] = Await.result(processedJobs.limit(10).runWith(Sink.seq), 3.seconds).toSet
  assert(result == list.map(_ + " done").toSet)
  result foreach println


  Await.ready(system.terminate(), 3.seconds)
  println("-----\n")
}
