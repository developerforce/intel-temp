package jobs

import java.io.File

import play.api._
import services.{Salesforce, Axeda}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

class Main(app: Application) {

  def run: Future[_] = {

    val tempSensorId = "59"
    val refrigId = "a05j00000028Fet"

    val axeda = Axeda(app)
    val salesforce = Salesforce(app)

    for {
      temp <- axeda.temperature(tempSensorId)
      refrig <- salesforce.getRefrig(refrigId)
      latest = (refrig \ "Latest_Temperature__c").asOpt[Float].getOrElse(0.0)

      if temp != latest

      high = (refrig \ "High_Temperature_Threshold__c").as[Float]
      low = (refrig \ "Low_Temperature_Threshold__c").as[Float]

      tempInsert <- salesforce.insertRefrigTemp(refrigId, temp)

      if (temp > high) || (temp < low)
      refrigCases <- salesforce.getRefrigCases(refrigId)

      if refrigCases.value.size == 0
      caseInsert <- salesforce.createRefrigCase(refrigId, temp)
    } yield temp
  }

}

object Main {
  def apply(implicit app: Application) = new Main(app)
}