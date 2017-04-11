/*
 * Copyright 2017 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.preferencesadminfrontend.connectors

import javax.inject.{Inject, Singleton}

import play.api.libs.json._
import play.api.libs.ws.WSClient
import play.api.libs.functional.syntax._
import uk.gov.hmrc.play.config.inject.ServicesConfig
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.preferencesadminfrontend.services.model.{Email, TaxIdentifier}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EntityResolverConnector @Inject()(wsClient: WSClient, serviceConfiguration: ServicesConfig) {

  def serviceUrl = serviceConfiguration.baseUrl("entity-resolver")

  def regimeFor(taxId: TaxIdentifier) = {
    taxId.name match {
      case "sautr" => "sa"
      case "nino" => "paye"
    }
  }

  def getTaxIdentifiers(taxId: TaxIdentifier)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Seq[TaxIdentifier]] = {
    val request = wsClient.url(s"$serviceUrl/entity-resolver/${regimeFor(taxId)}/${taxId.value}")

    val response = request.get()
    val result = response.map( _.json.as[JsObject]).map { jObj =>
      jObj.-("_id").fields.collect {
        case (name, JsString(value)) => TaxIdentifier(name, value)
      }
    }
    result
  }

  def getPreference(taxId: TaxIdentifier)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[PreferenceDetails]] = {
    val request = wsClient.url(s"$serviceUrl/portal/preferences/${regimeFor(taxId)}/${taxId.value}")
    val response = request.get()
    response.map( _.json.asOpt[PreferenceDetails])
  }
}

case class PreferenceDetails(paperless: Boolean, email: Email)

object PreferenceDetails {

  implicit val reads : Reads[PreferenceDetails] = (
    (JsPath \ "digital").read[Boolean] and
      (JsPath \ "email" \ "email").read[String] and
      (JsPath \ "email" \ "status").read[String]
    ) ((paperless, address, status) => {
    val verified = status == "verified"
    PreferenceDetails(paperless, Email(address, verified))
  })
}
