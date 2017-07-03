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

import play.api.Logger
import play.api.http.Status
import play.api.libs.functional.syntax._
import play.api.libs.json._
import uk.gov.hmrc.play.config.inject.ServicesConfig
import uk.gov.hmrc.play.http._
import uk.gov.hmrc.play.http.hooks.HttpHook
import uk.gov.hmrc.play.http.ws.{WSGet, WSPost}
import uk.gov.hmrc.preferencesadminfrontend.services.model.{Email, TaxIdentifier}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EntityResolverConnector @Inject()(serviceConfiguration: ServicesConfig) extends WSGet with WSPost {

  implicit val ef = Entity.formats

  val hooks: Seq[HttpHook] = NoneRequired

  def serviceUrl = serviceConfiguration.baseUrl("entity-resolver")

  def getTaxIdentifiers(taxId: TaxIdentifier)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Seq[TaxIdentifier]] = {
    val response = GET[Option[Entity]](s"$serviceUrl/entity-resolver/${taxId.regime}/${taxId.value}")
    response.map(
      _.fold(Seq.empty[TaxIdentifier])(entity =>
        Seq(
          entity.sautr.map(TaxIdentifier("sautr", _)),
          entity.nino.map(TaxIdentifier("nino", _))
        ).flatten)
    ).recover {
      case ex: BadRequestException => Seq.empty
    }
  }

  def getPreferenceDetails(taxId: TaxIdentifier)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[PreferenceDetails]] = {
    GET[Option[PreferenceDetails]](s"$serviceUrl/portal/preferences/${taxId.regime}/${taxId.value}").recover {
      case ex: BadRequestException => None
    }
  }

  def optOut(taxId: TaxIdentifier)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[OptOutResult] = {

    def warnNotOptedOut(status: Int) = Logger.warn(s"Unable to manually opt-out ${taxId.name} user with id ${taxId.value}. Status: $status")

    POSTEmpty(s"$serviceUrl/entity-resolver-admin/manual-opt-out/${taxId.regime}/${taxId.value}")
      .map(_ => OptedOut)
      .recover {
        case ex: NotFoundException =>
          warnNotOptedOut(404)
          PreferenceNotFound
        case ex@Upstream4xxResponse(_, Status.CONFLICT, _, _) =>
          warnNotOptedOut(ex.upstreamResponseCode)
          AlreadyOptedOut
        case ex@Upstream4xxResponse(_, Status.PRECONDITION_FAILED, _, _) =>
          warnNotOptedOut(ex.upstreamResponseCode)
          PreferenceNotFound
      }
  }
}

trait OptOutResult

case object OptedOut extends OptOutResult

case object AlreadyOptedOut extends OptOutResult {
  val errorCode: String = "AlreadyOptedOut"
}

case object PreferenceNotFound extends OptOutResult {
  val errorCode: String = "PreferenceNotFound"
}


case class Entity(sautr: Option[String], nino: Option[String])

object Entity {
  val formats = Json.format[Entity]
}

case class PreferenceDetails(genericPaperless: Boolean, taxCreditsPaperless: Boolean, email: Option[Email])

object PreferenceDetails {

  implicit val reads: Reads[PreferenceDetails] = (
    (JsPath \ "termsAndConditions" \ "generic").readNullable[JsValue].map(_.fold(false)(m => (m \ "accepted").as[Boolean])) and
    (JsPath \ "termsAndConditions" \ "taxCredits").readNullable[JsValue].map(_.fold(false)(m => (m \ "accepted").as[Boolean])) and
      (JsPath \ "email").readNullable[Email]
    ) ((genericPaperless, taxCreditsPaperless, email) => PreferenceDetails(genericPaperless, taxCreditsPaperless, email))
}