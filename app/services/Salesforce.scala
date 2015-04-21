package services

import org.joda.time.DateTime
import play.api.Application
import play.api.libs.json.{JsArray, JsObject, Json, JsValue}
import play.api.libs.ws.{WSResponse, WSRequestHolder, WS}
import play.api.http.{Status, HeaderNames}
import play.api.mvc.Results.EmptyContent
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future

class Salesforce(implicit app: Application) {

  lazy val authFuture: Future[Auth] = {
    val params = Map(
      "grant_type" -> "password",
      "client_id" -> app.configuration.getString("salesforce.consumer.key").get,
      "client_secret" -> app.configuration.getString("salesforce.consumer.secret").get,
      "username" -> app.configuration.getString("salesforce.username").get,
      "password" -> app.configuration.getString("salesforce.password").get
    )

    WS.
      url("https://login.salesforce.com/services/oauth2/token").
      withQueryString(params.toSeq:_*).
      post(EmptyContent()).
      flatMap { response =>
        response.status match {
          case Status.OK =>
            Future.successful(Auth((response.json \ "access_token").as[String], (response.json \ "instance_url").as[String]))
          case _ =>
            Future.failed(new IllegalStateException(s"Auth Denied: ${response.body}"))
        }
      }
  }


  def ws(path: String): Future[WSRequestHolder] = {
    authFuture.map { auth =>
      WS.
        url(s"${auth.instance}/services/data/v32.0/$path").
        withHeaders(HeaderNames.AUTHORIZATION -> s"Bearer ${auth.token}")
    }
  }

  def insertRefrigTemp(refrig: String, value: Double): Future[JsValue] = {
    val json = Json.obj(
      "Refrigerator__c" -> refrig,
      "Moment__c" -> DateTime.now(),
      "Value__c" -> value
    )

    ws("sobjects/Refrigerator_Temperature_Reading__c").flatMap {
      _.post(json).flatMap { response =>
        response.status match {
          case Status.CREATED =>
            Future.successful(response.json)
          case _ =>
            Future.failed(new IllegalStateException(response.json.toString()))
        }
      }
    }
  }

  def getRefrigs: Future[JsArray] = {
    val soql = s"SELECT Id, Axeda_Device_ID__c, Latest_Temperature__c, High_Temperature_Threshold__c, Low_Temperature_Threshold__c FROM Refrigerator__c"

    ws(s"query/").flatMap {
      _.withQueryString("q" -> soql).get().flatMap { response =>
        response.status match {
          case Status.OK =>
            Future.successful((response.json \ "records").as[JsArray])
          case _ =>
            Future.failed(new IllegalStateException(response.json.toString()))
        }
      }
    }
  }

  def getRefrig(id: String): Future[JsValue] = {
    ws(s"sobjects/Refrigerator__c/$id").flatMap {
      _.get().flatMap { response =>
        response.status match {
          case Status.OK =>
            Future.successful(response.json)
          case _ =>
            Future.failed(new IllegalStateException(response.json.toString()))
        }
      }
    }
  }

}

object Salesforce {
  def apply(implicit app: Application) = new Salesforce()
}

case class Auth(token: String, instance: String)