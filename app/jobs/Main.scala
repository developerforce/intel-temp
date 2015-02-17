package jobs

import java.io.File

import play.api.{Logger, Play, Mode, DefaultApplication}
import services.{Salesforce, Axeda}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

object Main extends App {

  implicit val app = new DefaultApplication(new File("."), Main.getClass.getClassLoader, null, Mode.Prod)

  Play.start(app)

  val tempSensorId = "59"
  val refrigId = "a05j00000028Fet"

  val axeda = Axeda(app)
  val salesforce = Salesforce(app)

  val job = for {
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

  job.onComplete {
    case Success(s) =>
      Play.stop()
    case Failure(e) =>
      Logger.error(e.getMessage)
      Play.stop()
  }

}
