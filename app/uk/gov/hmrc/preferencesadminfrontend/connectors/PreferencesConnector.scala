/*
 * Copyright 2019 HM Revenue & Customs
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

import akka.actor.ActorSystem
import javax.inject.{Inject, Singleton}
import play.api.{Configuration, Environment}
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.audit.DefaultAuditConnector
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient
import uk.gov.hmrc.preferencesadminfrontend.services.model.TaxIdentifier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PreferencesConnector @Inject()(frontendAuditConnector: DefaultAuditConnector,
                                     environment: Environment,
                                     val http: DefaultHttpClient,
                                     val runModeConfiguration: Configuration,
                                     val servicesConfig: ServicesConfig,
                                     val actorSystem: ActorSystem) {

  implicit val ef = Entity.formats

  def serviceUrl = servicesConfig.baseUrl("preferences")

  def getPreferenceDetails(taxId: TaxIdentifier)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[List[PreferenceDetails]] = {
    http.GET[List[PreferenceDetails]](s"$serviceUrl/preferences/email/${taxId.value}").recover {
      case _: BadRequestException => Nil
    }
  }
}