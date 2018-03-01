/*
 * Copyright 2018 HM Revenue & Customs
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
import uk.gov.hmrc.play.config.inject.ServicesConfig
import uk.gov.hmrc.play.http.ws.{WSGet, WSPost}
import uk.gov.hmrc.preferencesadminfrontend.services.model.{Email, TaxIdentifier}

import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.hooks.HttpHook
import uk.gov.hmrc.preferencesadminfrontend.FrontendAuditConnector
import uk.gov.hmrc.play.audit.http.HttpAuditing
import uk.gov.hmrc.play.config.AppName

@Singleton
class PreferencesConnector @Inject()(serviceConfiguration: ServicesConfig, frontendAuditConnector: FrontendAuditConnector) extends HttpGet with WSGet
  with HttpPost with WSPost with HttpAuditing with AppName {

  implicit val ef = Entity.formats

  val hooks: Seq[HttpHook] = Seq()

  override val auditConnector = frontendAuditConnector

  def serviceUrl = serviceConfiguration.baseUrl("preferences")

  def getPreferenceDetails(taxId: TaxIdentifier)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[List[PreferenceDetails]] = {
    GET[List[PreferenceDetails]](s"$serviceUrl/preferences/email/${taxId.value}").recover {
      case _: BadRequestException => Nil
    }
  }
}