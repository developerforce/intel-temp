package jobs

import java.io.File

import play.api.libs.concurrent.Akka
import play.api.{Play, Mode, DefaultApplication}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

object Tick extends App {

  implicit val app = new DefaultApplication(new File("."), Once.getClass.getClassLoader, null, Mode.Prod)

  Play.start(app)

  val main = Main(app)

  Akka.system.scheduler.schedule(Duration.Zero, 20.seconds) {
    main.run
  }

}
