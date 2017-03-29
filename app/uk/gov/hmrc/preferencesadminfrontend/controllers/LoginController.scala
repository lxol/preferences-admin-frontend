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

import play.api.Play.current
import play.api.data.Form
import play.api.data.Forms.{mapping, _}
import play.api.i18n.Messages.Implicits._
import play.api.mvc._
import uk.gov.hmrc.play.frontend.controller.FrontendController
import uk.gov.hmrc.preferencesadminfrontend.controllers.model.User
import uk.gov.hmrc.preferencesadminfrontend.services.LoginService

import scala.concurrent.Future

@Singleton
class LoginController @Inject()(loginService: LoginService) extends FrontendController {

  val showLoginPage = Action.async { implicit request =>
    Future.successful(Ok(uk.gov.hmrc.preferencesadminfrontend.views.html.login(userForm)))
  }

  val login = Action.async { implicit request =>
    userForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(uk.gov.hmrc.preferencesadminfrontend.views.html.login(formWithErrors)))
      },
      userData => {
        if (loginService.login(userData))
          Future.successful(Ok(uk.gov.hmrc.preferencesadminfrontend.views.html.customer_identification()))
        else
          Future.successful(Unauthorized(uk.gov.hmrc.preferencesadminfrontend.views.html.login(userForm)))
      }
    )
  }

  val userForm = Form(
    mapping(
      "username" -> text,
      "password" -> text
    )(User.apply)(User.unapply)
  )
}
