package jobs

import java.io.File

import play.api.{Logger, Play, Mode, DefaultApplication}

import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global

object Once extends App {

  implicit val app = new DefaultApplication(new File("."), Once.getClass.getClassLoader, null, Mode.Prod)

  Play.start(app)

  Main(app).run.onComplete {
    case Success(s) =>
      Play.stop()
    case Failure(e) =>
      Logger.error(e.getMessage)
      Play.stop()
  }

}
