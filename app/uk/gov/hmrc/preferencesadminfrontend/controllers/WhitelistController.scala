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

package uk.gov.hmrc.preferencesadminfrontend.controllers

import javax.inject.Inject
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import uk.gov.hmrc.preferencesadminfrontend.config.AppConfig
import uk.gov.hmrc.preferencesadminfrontend.connectors.MessageConnector
import uk.gov.hmrc.preferencesadminfrontend.model.{Whitelist, WhitelistEntry}
import uk.gov.hmrc.preferencesadminfrontend.model.Whitelist._
import uk.gov.hmrc.preferencesadminfrontend.views.html.{error_template, whitelist_add, whitelist_show}

import scala.concurrent.Future

class WhitelistController @Inject()(messageConnector: MessageConnector)
                                   (implicit appConfig: AppConfig, val messagesApi: MessagesApi) extends FrontendController with I18nSupport {

  def showWhitelistPage: Action[AnyContent] = AuthorisedAction.async {
    implicit request =>
      implicit user =>
        messageConnector.getWhitelist.map(response =>
          response.status match {
            case OK =>
              Json.parse(response.body).validate[Whitelist].asOpt match {
                case Some(whitelist) => Ok(whitelist_show(whitelist))
                case None => BadGateway(error_template("Error","There was an error:","The whitelist does not appear to be valid.",appConfig))
              }
            case _ => BadGateway(error_template("Error","There was an error:",response.body,appConfig))
          }
        )
  }

  def addFormId: Action[AnyContent] = AuthorisedAction.async {
    implicit request =>
      implicit user =>
        Future.successful(Ok(uk.gov.hmrc.preferencesadminfrontend.views.html.whitelist_add(WhitelistEntry())))
  }

  def confirmAdd: Action[AnyContent] = AuthorisedAction.async {
    implicit request =>
      implicit user =>
        WhitelistEntry().bindFromRequest().fold(
          formWithErrors => {
            Future.successful(BadRequest(whitelist_add(formWithErrors)))
          },
          addEntry => {
            messageConnector.addFormIdToWhitelist(addEntry).map(response =>
              response.status match {
                case CREATED => Redirect(routes.WhitelistController.showWhitelistPage())
                case _ => BadGateway(error_template("Error","There was an error:",response.body,appConfig))
              }
            )
          }
        )
  }

  def deleteFormId(formId:String): Action[AnyContent] = AuthorisedAction.async {
    implicit request =>
      implicit user =>
        Future.successful(Ok(uk.gov.hmrc.preferencesadminfrontend.views.html.whitelist_delete(WhitelistEntry().fill(WhitelistEntry(formId,"")))))
  }

  def confirmDelete: Action[AnyContent] = AuthorisedAction.async {
    implicit request =>
      implicit user =>
        WhitelistEntry().bindFromRequest().fold(
          formWithErrors => {
            Future.successful(BadRequest(uk.gov.hmrc.preferencesadminfrontend.views.html.whitelist_delete(formWithErrors)))
          },
          deleteEntry => {
            messageConnector.deleteFormIdFromWhitelist(deleteEntry).map(response =>
              response.status match {
                case OK => Redirect(routes.WhitelistController.showWhitelistPage())
                case _ => BadGateway(error_template("Error","There was an error:",response.body,appConfig))
              }
            )
          }
        )
  }

}
