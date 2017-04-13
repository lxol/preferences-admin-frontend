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
import play.api.mvc.Result
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.{AuditEvent, DataEvent}
import uk.gov.hmrc.play.config.AppName
import uk.gov.hmrc.play.frontend.controller.FrontendController
import uk.gov.hmrc.preferencesadminfrontend.config.AppConfig
import uk.gov.hmrc.preferencesadminfrontend.controllers.model.User
import uk.gov.hmrc.preferencesadminfrontend.services._
import uk.gov.hmrc.preferencesadminfrontend.services.model.TaxIdentifier

import scala.concurrent.Future

@Singleton
class SearchController @Inject()(auditConnector: AuditConnector, searchService: SearchService)
                                (implicit appConfig: AppConfig, val messagesApi: MessagesApi) extends FrontendController with AppName with I18nSupport {

  val showSearchPage = AuthorisedAction.async {
    implicit request => user => Future.successful(Ok(uk.gov.hmrc.preferencesadminfrontend.views.html.customer_identification()))
  }

  val search = AuthorisedAction.async {
    implicit request =>
      user =>
        val taxIdentifierName = request.getQueryString("taxIdentifierName").getOrElse("")
        val taxIdentifierValue = request.getQueryString("taxIdentifierValue").getOrElse("")
        val searchTaxIdentifier = TaxIdentifier(taxIdentifierName, taxIdentifierValue)

        searchService.getPreference(searchTaxIdentifier).map { p =>
          auditConnector.sendEvent(auditResult(user, p, searchTaxIdentifier))
          p match {
            case PreferenceFound(preference) => Ok(uk.gov.hmrc.preferencesadminfrontend.views.html.user_summary(searchTaxIdentifier, preference))
            case PreferenceNotFound => redirectToError("notfound", searchTaxIdentifier)
            case InvalidTaxIdentifier => redirectToError("invalidTaxId", searchTaxIdentifier)
            case Failure(reason) => redirectToError("genericError", searchTaxIdentifier)
          }
        }

  }

  def redirectToError(tag: String, taxIdentifier: TaxIdentifier): Result = Redirect(s"${routes.SearchController.showSearchPage.url}?err=$tag&taxIdentifierName=${taxIdentifier.name}&taxIdentifierValue=${taxIdentifier.value}")

  private def auditResult(user: User, preferenceResult: PreferenceResult, taxIdentifier: TaxIdentifier): AuditEvent = {
    preferenceResult match {
      case PreferenceFound(_) => createSearchEvent(user.username, taxIdentifier, successful = true, "PreferenceFound")
      case PreferenceNotFound => createSearchEvent(user.username, taxIdentifier, successful = true, "PreferenceNotFound")
      case InvalidTaxIdentifier => createSearchEvent(user.username, taxIdentifier, successful = false, "InvalidTaxIdentifier")
      case Failure(reason) => createSearchEvent(user.username, taxIdentifier, successful = false, "Failure")
    }
  }

  def createSearchEvent(username: String, taxIdentifier: TaxIdentifier, successful: Boolean, searchResult: String) = DataEvent(
    auditSource = appName,
    auditType = if (successful) "TxSucceeded" else "TxFailed",
    detail = Map("user" -> username, taxIdentifier.name -> taxIdentifier.value, "result" -> searchResult),
    tags = Map("transactionName" -> "Search")
  )
}
