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
import com.typesafe.config.ConfigException.Missing
import play.api.Configuration
import play.api.mvc.{Filter, RequestHeader, Result, Session}
import uk.gov.hmrc.play.http.SessionKeys
import uk.gov.hmrc.preferencesadminfrontend.controllers.routes
import uk.gov.hmrc.time.DateTimeUtils

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class AuthFilter @Inject()(configuration: Configuration)(implicit val mat: Materializer, ec: ExecutionContext) extends Filter {

  import AuthFilter._

  val accessRestrictedPaths = Seq(
    routes.SearchController.showSearchPage().url,
    routes.LoginController.logout().url
  )

  val entryPoint = routes.LoginController.showLoginPage().url

  val sessionTimeout = configuration.getInt("userSessionTimeoutInMillis").getOrElse(throw new Missing("Property userSessionTimeoutInMillis"))

  override def apply(nextFilter: (RequestHeader) => Future[Result])(requestHeader: RequestHeader): Future[Result] = {

    if (!accessRestrictedPaths.contains(requestHeader.path)) return nextFilter(requestHeader)
    if (isExpired(requestHeader.session, sessionTimeout)) {
      Future.successful(play.api.mvc.Results.Redirect(routes.LoginController.showLoginPage()).withSession(Session()))
    }
    else {
      val result =
        if (!userDefinedIn(requestHeader.session)) Future.successful(play.api.mvc.Results.Redirect(routes.LoginController.showLoginPage().url))
        else nextFilter(requestHeader)
      result.map(_.addingToSession((SessionKeys.lastRequestTimestamp, DateTimeUtils.now.getMillis.toString))(requestHeader))
    }
  }
}

object AuthFilter {
  def userDefinedIn(session: Session): Boolean =  session.get("user").isDefined

  def isExpired(session: Session, sessionTimeoutMillis: Int): Boolean = {
    session.get(SessionKeys.lastRequestTimestamp) match {
      case None => true
      case Some(timestamp) => timestamp.toLong < DateTimeUtils.now.minusMillis(sessionTimeoutMillis).getMillis
    }
  }

  def updateTimestampFor(session: Session, dateTimeUtils: DateTimeUtils): Session = session.+(SessionKeys.lastRequestTimestamp -> dateTimeUtils.now.getMillis.toString)
}
