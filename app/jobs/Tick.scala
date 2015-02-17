package jobs

import java.io.File

import play.api.libs.concurrent.Akka
import play.api.{Logger, Play, Mode, DefaultApplication}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

object Tick extends App {

  implicit val app = new DefaultApplication(new File("."), Once.getClass.getClassLoader, null, Mode.Prod)

  Play.start(app)

  val main = Main(app)

  Akka.system.scheduler.schedule(Duration.Zero, 20.seconds) {
    main.run.onComplete {
      case Success(s) =>
        Logger.info(s.toString)
      case Failure(e) =>
        Logger.error(e.getMessage)
    }
  }

}
