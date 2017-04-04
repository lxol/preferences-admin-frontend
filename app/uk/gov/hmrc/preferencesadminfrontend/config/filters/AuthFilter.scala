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

package uk.gov.hmrc.preferencesadminfrontend.config.filters

import javax.inject.{Inject, Singleton}

import akka.stream.Materializer
import play.api.mvc.{Filter, RequestHeader, Result, Session}
import uk.gov.hmrc.preferencesadminfrontend.controllers.{LoginController, routes}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AuthFilter @Inject()(implicit val mat: Materializer, ec: ExecutionContext) extends Filter {

  val accessRestrictedPaths = Seq(
    routes.SearchController.showSearchPage().url,
    routes.LoginController.logout().url
  )

  override def apply(nextFilter: (RequestHeader) => Future[Result])(requestHeader: RequestHeader): Future[Result] = {
    if (accessRestrictedPaths contains requestHeader.path) {
      if (!userDefinedIn(requestHeader.session)) Future.successful(play.api.mvc.Results.Redirect(routes.LoginController.showLoginPage().url))
      else nextFilter(requestHeader)
    }
    else nextFilter(requestHeader)
  }

  def userDefinedIn(session: Session): Boolean = {
    session.get("user").isDefined
  }
}
