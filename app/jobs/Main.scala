package jobs

import play.api._
import services.{Salesforce, Axeda}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class Main(app: Application) {

  def run: Future[_] = {

    val axeda = Axeda(app)
    val salesforce = Salesforce(app)

    for {
      refrigsJson <- salesforce.getRefrigs

      refrigs <- Future.sequence {
        refrigsJson.value.map { js =>
          val sfId = (js \ "Id").as[String]
          val axedaId = (js \ "Axeda_Device_ID__c").as[Int]
          val latestTemp = (js \ "Latest_Temperature__c").asOpt[Double].getOrElse(0.0)
          axeda.temperature(axedaId).map { currentTemp =>
            (sfId, latestTemp, currentTemp)
          }
        }
      }

      needsUpdate = refrigs.filter { case (_, latestTemp, currentTemp) =>
        latestTemp != currentTemp
      }

      inserts <- Future.sequence {
        needsUpdate.map { case (sfId, _, temp) =>
          salesforce.insertRefrigTemp(sfId, temp)
        }
      }

    } yield inserts
  }

}

object Main {
  def apply(implicit app: Application) = new Main(app)
}