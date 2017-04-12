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

import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.preferencesadminfrontend.connectors.EntityResolverConnector
import uk.gov.hmrc.preferencesadminfrontend.services.model.{Preference, TaxIdentifier}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SearchService @Inject()(entityResolverConnector: EntityResolverConnector) {

  def isValid(taxId: TaxIdentifier) : Boolean = {
    taxId match {
      case TaxIdentifier("nino",value) => Nino.isValid(value)
      case _ => true
    }
  }

  def getPreference(taxId: TaxIdentifier)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[PreferenceResult] = {
    (for {
      preferenceDetail <- entityResolverConnector.getPreferenceDetails(taxId)
      taxIdentifiers <- entityResolverConnector.getTaxIdentifiers(taxId)
    } yield (preferenceDetail, taxIdentifiers) match {
      case (Some(preferenceDetails), taxIds) => PreferenceFound(Preference(preferenceDetails.paperless, preferenceDetails.email, taxIds))
      case (None, _) => PreferenceNotFound
    }).recover {
      case t: Throwable => Failure(t.getMessage)
    }
  }

}

trait PreferenceResult

case class PreferenceFound(preference: Preference) extends PreferenceResult

case object PreferenceNotFound extends PreferenceResult

case class Failure(reason: String) extends PreferenceResult
