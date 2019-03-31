package doc.stream.modularity

import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl._

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}


object ComplexGraph extends App {

  implicit val system: ActorSystem = ActorSystem("SimpleGraph")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContext = system.dispatcher

  println("\n-----")


  //#complex-graph
  RunnableGraph.fromGraph(GraphDSL.create() { implicit builder =>
    import GraphDSL.Implicits._
    val A: Outlet[Int]                  = builder.add(Source.single(0)).out
    val B: UniformFanOutShape[Int, Int] = builder.add(Broadcast[Int](2))
    val C: UniformFanInShape[Int, Int]  = builder.add(Merge[Int](2))
    val D: FlowShape[Int, Int]          = builder.add(Flow[Int].map(_ + 1))
    val E: UniformFanOutShape[Int, Int] = builder.add(Balance[Int](2))
    val F: UniformFanInShape[Int, Int]  = builder.add(Merge[Int](2))
    val G: Inlet[Any]                   = builder.add(Sink.foreach(println)).in

    C     <~      F
    A  ~>  B  ~>  C     ~>      F
    B  ~>  D  ~>  E  ~>  F
    E  ~>  G

    ClosedShape
  }).run()


  //#complex-graph-alt
  RunnableGraph.fromGraph(GraphDSL.create() { implicit builder =>
    import GraphDSL.Implicits._
    val B = builder.add(Broadcast[Int](2))
    val C = builder.add(Merge[Int](2))
    val E = builder.add(Balance[Int](2))
    val F = builder.add(Merge[Int](2))

    Source.single(0) ~> B.in; B.out(0) ~> C.in(1); C.out ~> F.in(0)
    C.in(0) <~ F.out

    B.out(1).map(_ + 1) ~> E.in; E.out(0) ~> F.in(1)
    E.out(1) ~> Sink.foreach(println)
    ClosedShape
  }).run()


  //Await.result(result, 3.seconds)
  Await.result(system.terminate(), 3.seconds)

  println("-----\n")
}
