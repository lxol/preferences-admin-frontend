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

package uk.gov.hmrc.preferencesadminfrontend.controllers

import javax.inject.{Inject, Singleton}

import play.api.i18n.{I18nSupport, MessagesApi}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.DataEvent
import uk.gov.hmrc.play.config.AppName
import uk.gov.hmrc.play.frontend.controller.FrontendController
import uk.gov.hmrc.preferencesadminfrontend.config.AppConfig
import uk.gov.hmrc.preferencesadminfrontend.services.model.TaxIdentifier
import uk.gov.hmrc.preferencesadminfrontend.services.{Failure, PreferenceFound, PreferenceNotFound, SearchService}

import scala.concurrent.Future

@Singleton
class SearchController @Inject()(auditConnector: AuditConnector, searchService: SearchService)
                                (implicit appConfig: AppConfig, val messagesApi: MessagesApi) extends FrontendController with AppName with I18nSupport {

  val showSearchPage = AuthorisedAction.async {
   implicit request => user => Future.successful(Ok (uk.gov.hmrc.preferencesadminfrontend.views.html.customer_identification () ))
  }

  def search(taxIdentifierType: String, taxId: String) = AuthorisedAction.async {
    implicit request => user =>
      searchService.getPreference(TaxIdentifier(taxIdentifierType, taxId)).map {
        case PreferenceFound(preference) => {
          Ok(uk.gov.hmrc.preferencesadminfrontend.views.html.user_summary(preference))
        }
        case PreferenceNotFound => Ok
        case Failure(reason) => Ok
      }
  }

  def createSearchEvent(username: String, successful: Boolean) = DataEvent(
    auditSource = appName,
    auditType = if (successful) "TxSucceeded" else "TxFailed",
    detail = Map("user" -> username),
    tags = Map("transactionName" -> "Login")
  )
}
