package doc.stream.customstages

import java.util.concurrent.ThreadLocalRandom

import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl._
import akka.testkit.EventFilter
import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}


object OperatorWithLogging extends App {

  val config: Config = ConfigFactory.parseString("akka.loglevel = DEBUG")

  implicit val system: ActorSystem = ActorSystem("OperatorWithLogging", ConfigFactory.load(config))
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContext = system.dispatcher

  println("\n-----")


  //#operator-with-logging
  import akka.stream.stage.{ GraphStage, GraphStageLogic, OutHandler, StageLogging }

  final class RandomLettersSource extends GraphStage[SourceShape[String]] {
    private val out = Outlet[String]("RandomLettersSource.out")
    override val shape: SourceShape[String] = SourceShape(out)

    override def createLogic(inheritedAttributes: Attributes): GraphStageLogic with StageLogging =

      new GraphStageLogic(shape) with StageLogging {
        setHandler(out, new OutHandler {
          override def onPull(): Unit = {
            val c = nextChar() // ASCII lower case letters

            // `log` is obtained from materializer automatically (via StageLogging)
            log.debug("Randomly generated: [{}]", c)

            push(out, c.toString)
          }
        })
      }

    def nextChar(): Char =
      ThreadLocalRandom.current().nextInt('a', 'z'.toInt + 1).toChar
  }

  val n = 10
  val evtlyDone = //EventFilter.debug(start = "Randomly generated", occurrences = n).intercept {
    Source.fromGraph(new RandomLettersSource)
      .take(n)
      .runWith(Sink.ignore)
  //}

  println(Await.result(evtlyDone, 1.second))


  Await.result(system.terminate(), 3.seconds)

  println("-----\n")
}
