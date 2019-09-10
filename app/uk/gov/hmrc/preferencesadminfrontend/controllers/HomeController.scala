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

import javax.inject.{Inject, Singleton}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import uk.gov.hmrc.play.config.AppName
import uk.gov.hmrc.preferencesadminfrontend.config.AppConfig

import scala.concurrent.Future

@Singleton
class HomeController @Inject()(auditConnector: AuditConnector, appName: AppName)(
  implicit appConfig: AppConfig, val messagesApi: MessagesApi) extends FrontendController with I18nSupport {

  def showHomePage: Action[AnyContent] = Action.async {
    implicit request =>
      Future.successful(Ok(uk.gov.hmrc.preferencesadminfrontend.views.html.home()))
  }
}