package doc.stream.cookbook

import java.security.MessageDigest

import akka.NotUsed
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import akka.stream.{Attributes, FlowShape, Inlet, Outlet}
import akka.util.ByteString

import scala.concurrent.Await
import scala.concurrent.duration._

object App07CalculatingDigest extends AbstractApp {

  val data: Source[ByteString, NotUsed] = Source.single(ByteString("abc"))

  class DigestCalculator(algorithm: String) extends GraphStage[FlowShape[ByteString, ByteString]] {
    private val in = Inlet[ByteString]("DigestCalculator.in")
    private val out = Outlet[ByteString]("DigestCalculator.out")
    override val shape = FlowShape(in, out)

    override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {
      private val digest = MessageDigest.getInstance(algorithm)

      setHandler(out, new OutHandler {
        override def onPull(): Unit = pull(in)
      })

      setHandler(in, new InHandler {
        override def onPush(): Unit = {
          val chunk = grab(in)
          digest.update(chunk.toArray)
          pull(in)
        }

        override def onUpstreamFinish(): Unit = {
          emit(out, ByteString(digest.digest()))
          completeStage()
        }
      })
    }
  }

  val digestSource: Source[ByteString, NotUsed] = data.via(new DigestCalculator("SHA-256"))

  private val digest: ByteString = Await.result(digestSource.runWith(Sink.head), 3.seconds)
  println(digest)

  assert(digest ==
    ByteString(0xba, 0x78, 0x16, 0xbf, 0x8f, 0x01, 0xcf, 0xea, 0x41, 0x41, 0x40, 0xde, 0x5d, 0xae, 0x22, 0x23, 0xb0,
      0x03, 0x61, 0xa3, 0x96, 0x17, 0x7a, 0x9c, 0xb4, 0x10, 0xff, 0x61, 0xf2, 0x00, 0x15, 0xad))


  Await.ready(system.terminate(), 3.seconds)
  println("-----\n")
}
