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

package uk.gov.hmrc.preferencesadminfrontend.services

import javax.inject.{Inject, Singleton}

import play.api.libs.json.Json
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.preferencesadminfrontend.connectors.EntityResolverConnector
import uk.gov.hmrc.preferencesadminfrontend.services.model.{Preference, TaxIdentifier}
import uk.gov.hmrc.time.DateTimeUtils

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SearchService @Inject()(entityResolverConnector: EntityResolverConnector, auditConnector: AuditConnector) {

  def getPreference(taxId: TaxIdentifier)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[Preference]] = {
    for {
      preferenceDetail <- entityResolverConnector.getPreferenceDetails(taxId)
      taxIdentifiers <- entityResolverConnector.getTaxIdentifiers(taxId)
    } yield preferenceDetail.map(details => Preference(details.paperless, details.email, taxIdentifiers))
  }

  def optOut(taxId: TaxIdentifier)(implicit hc: HeaderCarrier, ec: ExecutionContext) : Future[Boolean] = {
    val dataEvent = ExtendedDataEvent("","","", Map.empty, Json.obj(),DateTimeUtils.now)
    entityResolverConnector.optOut(taxId).map {
      response =>
        auditConnector.sendEvent(dataEvent)
        response
    }
  }
}
