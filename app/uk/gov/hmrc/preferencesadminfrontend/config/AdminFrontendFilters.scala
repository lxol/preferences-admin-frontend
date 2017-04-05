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

package uk.gov.hmrc.preferencesadminfrontend.config

import javax.inject.{Inject, Singleton}

import play.api.mvc.{Request, RequestHeader, Result}
import play.api.{Application, Configuration}
import play.twirl.api.Html
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.config.RunMode
import uk.gov.hmrc.play.frontend.bootstrap.DefaultFrontendGlobal
import uk.gov.hmrc.play.http.logging.filters.FrontendLoggingFilter
import uk.gov.hmrc.preferencesadminfrontend.config.filters.PreferencesFrontendAuditFilter

import scala.concurrent.Future

@Singleton
class AdminFrontendGlobal @Inject()(
     override val loggingFilter: FrontendLoggingFilter,
     override val frontendAuditFilter: PreferencesFrontendAuditFilter,
     override val auditConnector: AuditConnector) extends DefaultFrontendGlobal with RunMode {

  override def standardErrorTemplate(pageTitle: String, heading: String, message: String)(implicit request: Request[_]): Html = Html("")

  override def microserviceMetricsConfig(implicit app: Application): Option[Configuration] = app.configuration.getConfig(s"$env.microservice.metrics")

  override def onClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] = ???
}
