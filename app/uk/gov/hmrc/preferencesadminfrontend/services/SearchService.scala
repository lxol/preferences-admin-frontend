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

package uk.gov.hmrc.preferencesadminfrontend.services

import javax.inject.{Inject, Singleton}

import akka.actor.FSM.Reason
import play.api.libs.json.Json
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent
import uk.gov.hmrc.play.config.inject.AppName
import uk.gov.hmrc.preferencesadminfrontend.connectors._
import uk.gov.hmrc.preferencesadminfrontend.controllers.model.User
import uk.gov.hmrc.preferencesadminfrontend.services.model.{Preference, TaxIdentifier}

import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.http.HeaderCarrier

@Singleton
class SearchService @Inject()(entityResolverConnector: EntityResolverConnector, auditConnector: AuditConnector, appName: AppName) {

  def searchPreference(taxId: TaxIdentifier)(implicit user: User, hc: HeaderCarrier, ec: ExecutionContext): Future[Option[Preference]] = {
    val preferenceOpt = getPreference(taxId)
    preferenceOpt.foreach(preference => auditConnector.sendExtendedEvent(createSearchEvent(user.username, taxId, preference)))
    preferenceOpt
  }


  def getPreference(taxId: TaxIdentifier)(implicit user: User, hc: HeaderCarrier, ec: ExecutionContext): Future[Option[Preference]] = {
    for {
      preferenceDetail <- entityResolverConnector.getPreferenceDetails(taxId)
      taxIdentifiers <- entityResolverConnector.getTaxIdentifiers(taxId)
    } yield preferenceDetail.map(details => Preference(details.genericPaperless, details.genericUpdatedAt, details.taxCreditsPaperless, details.email, taxIdentifiers))
  }

  def optOut(taxId: TaxIdentifier, reason: String)(implicit user: User, hc: HeaderCarrier, ec: ExecutionContext) : Future[OptOutResult] = {

    for {
      originalPreference <- getPreference(taxId)
      optoutResult <- entityResolverConnector.optOut(taxId)
      newPreference <- getPreference(taxId)
    } yield {
      auditConnector.sendExtendedEvent(createOptOutEvent(user.username, taxId, originalPreference, newPreference, optoutResult, reason))
      optoutResult
    }

  }

  def createOptOutEvent(username: String, taxIdentifier: TaxIdentifier, originalPreference: Option[Preference], newPreference: Option[Preference],optOutResult: OptOutResult, reason: String) : ExtendedDataEvent = {
    val reasonOfFailureJson = optOutResult match {
      case OptedOut => Json.obj()
      case AlreadyOptedOut => Json.obj("reasonOfFailure" -> "Preference already opted out")
      case PreferenceNotFound => Json.obj("reasonOfFailure" -> "Preference not found")
    }

    val details = Json.obj(
      "user" -> username,
      "query" -> Json.toJson(taxIdentifier),
      "optOutReason" -> reason
    ) ++
      originalPreference.fold(Json.obj())(p => Json.obj("originalPreference" -> Json.toJson(p))) ++
      newPreference.fold(Json.obj())(p => Json.obj("newPreference" -> Json.toJson(p))) ++
      reasonOfFailureJson

    ExtendedDataEvent(
      auditSource = appName.appName,
      auditType = if (optOutResult == OptedOut) "TxSucceeded" else "TxFailed",
      detail = details,
      tags = Map("transactionName" -> "Manual opt out from paperless")
    )
  }

  def createSearchEvent(username: String, taxIdentifier: TaxIdentifier, preference: Option[Preference]): ExtendedDataEvent = {
    val details = Json.obj(
      "user" -> username,
      "query" -> Json.toJson(taxIdentifier),
      "result" -> preference.fold("Not found")(_ => "Found")
    ) ++ preference.fold(Json.obj())(p => Json.obj("preference" -> Json.toJson(p)))

    ExtendedDataEvent(
      auditSource = appName.appName,
      auditType = "TxSucceeded",
      detail = details,
      tags = Map("transactionName" -> "Paperless opt out search")
    )
  }
}
