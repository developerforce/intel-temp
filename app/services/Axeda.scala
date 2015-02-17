package services

import org.joda.time.DateTime
import play.api.Application
import play.api.libs.json.{JsArray, JsObject, Json, JsValue}
import play.api.libs.ws.{WSResponse, WSRequestHolder, WS}
import play.api.http.{MimeTypes, Status, HeaderNames}
import play.api.mvc.Results.EmptyContent
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future

class Axeda(implicit app: Application) {

  lazy val authFuture: Future[String] = {
    val params = Map(
      "principal.username" -> app.configuration.getString("axeda.username").get,
      "password" -> app.configuration.getString("axeda.password").get
    )

    WS
      .url("https://windriver.axeda.com/services/v1/rest/Auth/login")
      .withQueryString(params.toSeq:_*)
      .withHeaders(HeaderNames.ACCEPT -> MimeTypes.JSON)
      .get().
      flatMap { response =>
        response.status match {
          case Status.OK =>
            Future.successful((response.json \ "wsSessionInfo" \ "sessionId").as[String])
          case _ =>
            Future.failed(new IllegalStateException(s"Auth Denied: ${response.body}"))
        }
      }
  }

  def ws(path: String): Future[WSRequestHolder] = {
    authFuture.map { sessionid =>
      WS
        .url(s"https://windriver.axeda.com/$path")
        .withHeaders("x_axeda_wss_sessionid" -> sessionid, HeaderNames.ACCEPT -> MimeTypes.JSON)
    }
  }

  def asset(id: String): Future[JsValue] = {
    ws("services/v2/rest/asset/id/" + id).flatMap(_.get()).flatMap { response =>
      response.status match {
        case Status.OK =>
          Future.successful(response.json)
        case _ =>
          Future.failed(new IllegalStateException(response.body))
      }
    }
  }

  def temperature(id: String): Future[Float] = {
    val json = Json.obj(
      "assetId" -> 503,
      "name" -> "temperature"
    )

    ws("services/v2/rest/dataItem/findCurrentValues")
      .flatMap(_.post(json))
      .flatMap { response =>
      response.status match {
        case Status.OK =>
          (response.json \\ "value").headOption.map(_.as[String].toFloat).fold {
            Future.failed[Float](new IllegalStateException(response.body))
          } { temp =>
            Future.successful(temp)
          }
        case _ =>
          Future.failed(new IllegalStateException(response.body))
      }
    }
  }

}

object Axeda {
  def apply(implicit app: Application) = new Axeda()
}