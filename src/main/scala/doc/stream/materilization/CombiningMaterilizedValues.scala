package doc.stream.materilization

import akka.actor.{ActorSystem, Cancellable}
import akka.stream.{ActorMaterializer, ClosedShape, FanInShape2, FlowShape}
import akka.stream.scaladsl._
import akka.{Done, NotUsed}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future, Promise}


object CombiningMaterilizedValues extends App {

  implicit val system: ActorSystem = ActorSystem("CombiningMaterilizedValues")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ex: ExecutionContext = system.dispatcher

  println("\n-----")

  val throttler = Flow.fromGraph(GraphDSL.create(Source.tick(1.second, 1.second, "test")) { implicit builder ⇒ tickSource ⇒
    import GraphDSL.Implicits._
    val zip: FanInShape2[String, Int, Int] = builder.add(ZipWith[String, Int, Int](Keep.right))
    tickSource ~> zip.in0
    FlowShape(zip.in1, zip.out)
  })

  // An source that can be signalled explicitly from the outside
  val source: Source[Int, Promise[Option[Int]]] = Source.maybe[Int]

  // A flow that internally throttles elements to 1/second, and returns a Cancellable
  // which can be used to shut down the stream
  val flow: Flow[Int, Int, Cancellable] = throttler

  // A sink that returns the first element of a stream in the returned Future
  val sink: Sink[Int, Future[Int]] = Sink.head[Int]

  // By default, the materialized value of the leftmost stage is preserved
  val r1: RunnableGraph[Promise[Option[Int]]] = source.via(flow).to(sink)

  // Simple selection of materialized values by using Keep.right
  val r2: RunnableGraph[Cancellable] = source.viaMat(flow)(Keep.right).to(sink)
  val r3: RunnableGraph[Future[Int]] = source.via(flow).toMat(sink)(Keep.right)

  // Using runWith will always give the materialized values of the stages added
  // by runWith() itself
  val r4: Future[Int] = source.via(flow).runWith(sink)
  val r5: Promise[Option[Int]] = flow.to(sink).runWith(source)
  val r6: (Promise[Option[Int]], Future[Int]) = flow.runWith(source, sink)

  // Using more complex combinations
  val r7: RunnableGraph[(Promise[Option[Int]], Cancellable)] =
    source.viaMat(flow)(Keep.both).to(sink)

  val r8: RunnableGraph[(Promise[Option[Int]], Future[Int])] =
    source.via(flow).toMat(sink)(Keep.both)

  val r9: RunnableGraph[((Promise[Option[Int]], Cancellable), Future[Int])] =
    source.viaMat(flow)(Keep.both).toMat(sink)(Keep.both)

  val r10: RunnableGraph[(Cancellable, Future[Int])] =
    source.viaMat(flow)(Keep.right).toMat(sink)(Keep.both)

  // It is also possible to map over the materialized values. In r9 we had a
  // doubly nested pair, but we want to flatten it out
  val r11: RunnableGraph[(Promise[Option[Int]], Cancellable, Future[Int])] =
  r9.mapMaterializedValue {
    case ((promise, cancellable), future) ⇒ (promise, cancellable, future)
  }

  // Now we can use pattern matching to get the resulting materialized values
  val (promise, cancellable, future) = r11.run()

  // Type inference works as expected
  promise.success(Some(1))
  // cancellable.cancel()
  val result = future.map(_ + 3)
  result foreach println

  // The result of r11 can be also achieved by using the Graph API
  val r12: RunnableGraph[(Promise[Option[Int]], Cancellable, Future[Int])] =
    RunnableGraph.fromGraph(GraphDSL.create(source, flow, sink)((_, _, _)) { implicit builder ⇒ (src, f, dst) ⇒
      import GraphDSL.Implicits._
      src ~> f ~> dst
      ClosedShape
    })

  Thread sleep 1000L
  Await.result(result, 3.seconds)
  Await.result(system.terminate(), 3.seconds)

  println("-----\n")
}
