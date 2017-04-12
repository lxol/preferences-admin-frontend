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
        val taxIdType = request.getQueryString("taxIdentifierType").getOrElse("")
        val taxId = request.getQueryString("taxId").getOrElse("")
        val searchTaxIdentifier = TaxIdentifier(taxIdType, taxId)

        searchService.getPreference(searchTaxIdentifier).map {
          case PreferenceFound(preference) => {
            createSearchEvent(user.username, searchTaxIdentifier, successful = true)
            Ok(uk.gov.hmrc.preferencesadminfrontend.views.html.user_summary(searchTaxIdentifier, preference))
          }
          case PreferenceNotFound => {
            createSearchEvent(user.username, searchTaxIdentifier, successful = true)
            redirectToError("notfound")
          }
          case InvalidTaxIdentifier => {
            createSearchEvent(user.username, searchTaxIdentifier, successful = false)
            redirectToError("invalidTaxId")
          }
          case Failure(reason) => {
            createSearchEvent(user.username, searchTaxIdentifier, successful = false)
            redirectToError("genericError")
          }
        }
  }

  def redirectToError(tag: String) = Redirect(s"${routes.SearchController.showSearchPage.url}?err=$tag")

  def createSearchEvent(username: String, taxIdentifier: TaxIdentifier, successful: Boolean) = DataEvent(
    auditSource = appName,
    auditType = if (successful) "TxSucceeded" else "TxFailed",
    detail = Map("user" -> username, taxIdentifier.name -> taxIdentifier.value),
    tags = Map("transactionName" -> "Search")
  )
}
