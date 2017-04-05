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

import play.api.data.Form
import play.api.data.Forms.{mapping, _}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.DataEvent
import uk.gov.hmrc.play.config.AppName
import uk.gov.hmrc.play.frontend.controller.FrontendController
import uk.gov.hmrc.preferencesadminfrontend.FrontendAuditConnector
import uk.gov.hmrc.preferencesadminfrontend.config.AppConfig
import uk.gov.hmrc.preferencesadminfrontend.controllers.model.User
import uk.gov.hmrc.preferencesadminfrontend.services.LoginService
import uk.gov.hmrc.time.DateTimeUtils

import scala.concurrent.Future

@Singleton
class LoginController @Inject()(loginService: LoginService)(implicit appConfig: AppConfig, val messagesApi: MessagesApi) extends FrontendController with AppName with I18nSupport {

  def auditConnector: AuditConnector = FrontendAuditConnector

  val showLoginPage = Action.async {
    implicit request => Future.successful(Ok(uk.gov.hmrc.preferencesadminfrontend.views.html.login(userForm)))
  }

  val login = Action.async { implicit request =>
    userForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(uk.gov.hmrc.preferencesadminfrontend.views.html.login(formWithErrors))),
      userData => {
        if (loginService.isAuthorised(userData)) {
          auditConnector.sendEvent(createLoginEvent(userData.username, true))
          Future.successful(Redirect(routes.SearchController.showSearchPage.url).withSession(request.session + ("user" -> userData.username) + (uk.gov.hmrc.play.http.SessionKeys.lastRequestTimestamp -> DateTimeUtils.now.getMillis.toString)))
        }
        else {
          auditConnector.sendEvent(createLoginEvent(userData.username, false))
          Future.successful(Unauthorized(uk.gov.hmrc.preferencesadminfrontend.views.html.login(userForm)))
        }
      }
    )
  }

  val logout = Action.async { implicit request =>
    auditConnector.sendEvent(createLogoutEvent(request.session.get("user").get))
    Future.successful(Redirect(routes.LoginController.showLoginPage().url).withSession(new Session()))
  }

  def createLoginEvent(username: String, successful: Boolean) = DataEvent(
    auditSource = appName,
    auditType = if (successful) "TxSucceeded" else "TxFailed",
    detail = Map("user" -> username),
    tags = Map("transactionName" -> "Login")
  )

  def createLogoutEvent(username: String) = DataEvent(
    auditSource = appName,
    auditType = "TxSucceeded",
    detail = Map("user" -> username),
    tags = Map("transactionName" -> "Logout")
  )

  val userForm = Form(
    mapping(
      "username" -> text,
      "password" -> text
    )(User.apply)(User.unapply)
  )
}
