package doc.stream.cookbook

import akka.actor.ActorSystem
import akka.stream._

abstract class AkkaStreamApp extends App {

  val temp = getClass.getSimpleName
  val appName = temp.substring(0, temp.length - 1)

  implicit val system: ActorSystem = ActorSystem(appName)
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  println(s"\n----- $appName -----")

/*
  sys.addShutdownHook {
    Await.ready(system.terminate(), 3.seconds)
    println("-----\n")
  }
*/
}
