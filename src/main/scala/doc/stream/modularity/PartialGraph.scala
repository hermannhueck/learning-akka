package doc.stream.modularity

import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl._

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}


object PartialGraph extends App {

  implicit val system: ActorSystem = ActorSystem("SimpleGraph")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContext = system.dispatcher

  println("\n-----")


  //#partial-graph
  import GraphDSL.Implicits._
  val partial = GraphDSL.create() { implicit builder =>
    val B = builder.add(Broadcast[Int](2))
    val C = builder.add(Merge[Int](2))
    val E = builder.add(Balance[Int](2))
    val F = builder.add(Merge[Int](2))

    C  <~  F
    B  ~>                            C  ~>  F
    B  ~>  Flow[Int].map(_ + 1)  ~>  E  ~>  F
    FlowShape(B.in, E.out(1))
  }.named("partial")
  //#partial-graph
  // format: ON

  //#partial-use
  Source.single(0).via(partial).to(Sink.ignore).run()
  //#partial-use

  // format: OFF
  //#partial-flow-dsl
  // Convert the partial graph of FlowShape to a Flow to get
  // access to the fluid DSL (for example to be able to call .filter())
  val flow = Flow.fromGraph(partial)

  // Simple way to create a graph backed Source
  val source = Source.fromGraph( GraphDSL.create() { implicit builder =>
    val merge = builder.add(Merge[Int](2))
    Source.single(0)      ~> merge
    Source(List(2, 3, 4)) ~> merge

    // Exposing exactly one output port
    SourceShape(merge.out)
  })

  // Building a Sink with a nested Flow, using the fluid DSL
  val sink = {
    val nestedFlow = Flow[Int].map(_ * 2).drop(10).named("nestedFlow")
    nestedFlow.to(Sink.head)
  }

  // Putting all together
  val closed = source.via(flow.filter(_ > 1)).to(sink)
  closed.run()


  //Await.result(result, 3.seconds)
  Await.result(system.terminate(), 3.seconds)

  println("-----\n")
}
