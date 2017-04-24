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
import play.api.libs.json.Json
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent
import uk.gov.hmrc.play.config.AppName
import uk.gov.hmrc.play.frontend.controller.FrontendController
import uk.gov.hmrc.preferencesadminfrontend.config.AppConfig
import uk.gov.hmrc.preferencesadminfrontend.services._
import uk.gov.hmrc.preferencesadminfrontend.services.model.{Preference, Search, TaxIdentifier}

import scala.concurrent.Future

@Singleton
class SearchController @Inject()(auditConnector: AuditConnector, searchService: SearchService)
                                (implicit appConfig: AppConfig, val messagesApi: MessagesApi) extends FrontendController with AppName with I18nSupport {

  def showSearchPage(taxIdentifierName: String, taxIdentifierValue: String) = AuthorisedAction.async {
    implicit request => user => Future.successful(Ok(uk.gov.hmrc.preferencesadminfrontend.views.html.customer_identification(Search().bind(Map("name" -> taxIdentifierName, "value" -> taxIdentifierValue)).discardingErrors)))
  }

  def search = AuthorisedAction.async {
    implicit request =>
      user =>
        Search().bindFromRequest.fold(
          errors => Future.successful(BadRequest(uk.gov.hmrc.preferencesadminfrontend.views.html.customer_identification(errors))),
          searchTaxIdentifier => {

            searchService.getPreference(searchTaxIdentifier).map {
              case Some(preference) =>
                auditConnector.sendEvent(createSearchEvent(user.username, searchTaxIdentifier, Some(preference)))
                Ok(uk.gov.hmrc.preferencesadminfrontend.views.html.user_summary(searchTaxIdentifier, preference))
              case None =>
                auditConnector.sendEvent(createSearchEvent(user.username, searchTaxIdentifier, None))
                Ok(uk.gov.hmrc.preferencesadminfrontend.views.html.customer_identification(Search().bindFromRequest.withError("value", "error.preference_not_found")))
            }
          }
        )

  }


  def optOut(taxIdentifierName: String, taxIdentifierValue: String) = AuthorisedAction.async{
    implicit request => user =>
      searchService.optOut(TaxIdentifier(taxIdentifierName,taxIdentifierValue)).map(_ => Redirect(routes.SearchController.confirmed(taxIdentifierName, taxIdentifierValue)))
  }


  def confirmed(taxIdentifierName: String, taxIdentifierValue: String) = AuthorisedAction.async{
    implicit request => user => Future.successful(Ok("DONE"))
  }

  def createSearchEvent(username: String, taxIdentifier: TaxIdentifier, preference: Option[Preference]): ExtendedDataEvent = {
    val details = Json.obj(
      "user" -> username,
      "query" -> Json.toJson(taxIdentifier),
      "result" -> preference.fold("Not found")(_ => "Found")
    ) ++ preference.fold(Json.obj())(p => Json.obj("preference" -> Json.toJson(p)))

    ExtendedDataEvent(
      auditSource = appName,
      auditType = "TxSucceeded",
      detail = details,
      tags = Map("transactionName" -> "Paperless opt out search")
    )
  }
}
