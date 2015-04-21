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

      refrigs = refrigsJson.value.map { js =>
        val sfId = (js \ "Id").as[String]
        val axedaId = (js \ "Axeda_Device_ID__c").as[Int]
        sfId -> axedaId
      }

      refrigsWithTemp <- Future.sequence {
        refrigs.map { case (sfId, axedaId) =>
          for {
            temp <- axeda.temperature(axedaId)
            refrig <- salesforce.getRefrig(sfId)
            latest = (refrig \ "Latest_Temperature__c").asOpt[Float].getOrElse(0.0)

            if temp != latest

            high = (refrig \ "High_Temperature_Threshold__c").as[Float]
            low = (refrig \ "Low_Temperature_Threshold__c").as[Float]

            tempInsert <- salesforce.insertRefrigTemp(sfId, temp)
          } yield sfId ->(axedaId, temp)
        }
      }


      /*
      if (temp > high) || (temp < low)
      refrigCases <- salesforce.getRefrigCases(refrigId)

      if refrigCases.value.size == 0
      caseInsert <- salesforce.createRefrigCase(refrigId, temp)
      */
    } yield refrigsWithTemp
  }

}

object Main {
  def apply(implicit app: Application) = new Main(app)
}