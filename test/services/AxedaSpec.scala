package services

import org.scalatestplus.play._
import play.api.test.Helpers._

class AxedaSpec extends PlaySpec with OneAppPerSuite {

  val axeda = Axeda(app)

  "temperature" must {
    "return the temperature" in {
      val temp = await(axeda.temperature(762))
      temp must be > 0.0
    }
  }

}