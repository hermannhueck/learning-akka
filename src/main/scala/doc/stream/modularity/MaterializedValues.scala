package doc.stream.modularity

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl.Tcp.OutgoingConnection
import akka.stream.scaladsl._
import akka.util.ByteString

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future, Promise}


object MaterializedValues extends App {

  implicit val system: ActorSystem = ActorSystem("SimpleGraph")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContext = system.dispatcher

  println("\n-----")


  //#mat-combine-1
  // Materializes to Promise[Option[Int]]                                   (red)
  val source: Source[Int, Promise[Option[Int]]] = Source.maybe[Int]

  // Materializes to NotUsed                                               (black)
  val flow1: Flow[Int, Int, NotUsed] = Flow[Int].take(100)

  // Materializes to Promise[Int]                                          (red)
  val nestedSource: Source[Int, Promise[Option[Int]]] =
    source.viaMat(flow1)(Keep.left).named("nestedSource")
  //#mat-combine-1

  //#mat-combine-2
  // Materializes to NotUsed                                                (orange)
  val flow2: Flow[Int, ByteString, NotUsed] = Flow[Int].map { i ⇒ ByteString(i.toString) }

  // Materializes to Future[OutgoingConnection]                             (yellow)
  val flow3: Flow[ByteString, ByteString, Future[OutgoingConnection]] =
    Tcp().outgoingConnection("localhost", 8080)

  // Materializes to Future[OutgoingConnection]                             (yellow)
  val nestedFlow: Flow[Int, ByteString, Future[OutgoingConnection]] =
    flow2.viaMat(flow3)(Keep.right).named("nestedFlow")
  //#mat-combine-2

  //#mat-combine-3
  // Materializes to Future[String]                                         (green)
  val sink: Sink[ByteString, Future[String]] = Sink.fold("")(_ + _.utf8String)

  // Materializes to (Future[OutgoingConnection], Future[String])           (blue)
  val nestedSink: Sink[Int, (Future[OutgoingConnection], Future[String])] =
    nestedFlow.toMat(sink)(Keep.both)

  //#mat-combine-3

  //#mat-combine-4
  case class MyClass(private val p: Promise[Option[Int]], conn: OutgoingConnection) {
    def close(): Boolean = p.trySuccess(None)
  }

  def f(p: Promise[Option[Int]], rest: (Future[OutgoingConnection], Future[String])): Future[MyClass] = {
    val connFuture = rest._1
    connFuture.map(MyClass(p, _))
  }

  // Materializes to Future[MyClass]                                        (purple)
  val runnableGraph: RunnableGraph[Future[MyClass]] =
    nestedSource.toMat(nestedSink)(f)

  //val result = Await.result(runnableGraph.run(), 1.second)


  //Await.result(result, 3.seconds)
  Await.result(system.terminate(), 3.seconds)

  println("-----\n")
}
