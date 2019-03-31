package doc.stream.integration.external

import java.util.concurrent.atomic.AtomicInteger

import akka.NotUsed
import akka.actor.{ActorRef, ActorSystem}
import akka.stream._
import akka.stream.scaladsl._
import akka.testkit.TestProbe
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}


object SometimesSlowMapAsync extends App {

  val config = ConfigFactory.parseString("""
    #//#blocking-dispatcher-config
    blocking-dispatcher {
      executor = "thread-pool-executor"
      thread-pool-executor {
        core-pool-size-min    = 10
        core-pool-size-max    = 10
      }
    }
    #//#blocking-dispatcher-config

    akka.actor.default-mailbox.mailbox-type = akka.dispatch.UnboundedMailbox
    """)

  implicit val system: ActorSystem = ActorSystem("SometimesSlowMapAsync", config)
  //implicit val ec: ExecutionContext = system.dispatcher

  println("\n-----")


  class SometimesSlowService(implicit ec: ExecutionContext) {

    //def println(s: String): Unit = ()

    private val runningCount = new AtomicInteger

    def convert(s: String): Future[String] = {
      println(s"running: $s (${runningCount.incrementAndGet()})")
      Future {
        if (s.nonEmpty && s.head.isLower)
          Thread.sleep(500)
        else
          Thread.sleep(20)
        println(s"completed: $s (${runningCount.decrementAndGet()})")
        s.toUpperCase
      }
    }
  }

  val probe = TestProbe()
/*
  def println(s: String): Unit = {
    if (s.startsWith("after:"))
      probe.ref ! s
  }
*/

  val blockingExecutionContext: ExecutionContext = system.dispatchers.lookup("blocking-dispatcher")
  val service = new SometimesSlowService()(blockingExecutionContext)

  implicit val materializer: ActorMaterializer =
    ActorMaterializer(ActorMaterializerSettings(system).withInputBuffer(initialSize = 4, maxSize = 4))

  val done = Source(List("a", "B", "C", "D", "e", "F", "g", "H", "i", "J"))
    .map(elem => { println(s"before: $elem"); elem })
    .mapAsync(4)(service.convert)
    .runForeach(elem => println(s"after: $elem"))

/*
  probe.expectMsg("after: A")
  probe.expectMsg("after: B")
  probe.expectMsg("after: C")
  probe.expectMsg("after: D")
  probe.expectMsg("after: E")
  probe.expectMsg("after: F")
  probe.expectMsg("after: G")
  probe.expectMsg("after: H")
  probe.expectMsg("after: I")
  probe.expectMsg("after: J")
*/

  Await.ready(done, 3.seconds)


  Await.result(system.terminate(), 3.seconds)

  println("-----\n")
}
