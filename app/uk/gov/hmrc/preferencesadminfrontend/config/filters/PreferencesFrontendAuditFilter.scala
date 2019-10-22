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

package uk.gov.hmrc.preferencesadminfrontend.config.filters

import javax.inject.Inject
import akka.stream.Materializer
import com.google.inject.name.Named
import play.api.Configuration
import play.api.mvc.RequestHeader
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.DataEvent
import uk.gov.hmrc.play.bootstrap.config.{AppName, RunMode}
import uk.gov.hmrc.play.bootstrap.filters.frontend.FrontendAuditFilter
import uk.gov.hmrc.preferencesadminfrontend.FrontendAuditConnector

import scala.concurrent.ExecutionContext

class PreferencesFrontendAuditFilter @Inject()(configuration: Configuration,
                                               @Named("appName") apppName: String,
                                               runMode: RunMode,
                                               frontendAuditConnector: FrontendAuditConnector)(implicit val ec: ExecutionContext, val mat: Materializer) extends FrontendAuditFilter {

  override val maskedFormFields: Seq[String] = Seq("password")
  override val applicationPort: Option[Int] = None
  override lazy val auditConnector: AuditConnector = frontendAuditConnector

  override def controllerNeedsAuditing(controllerName: String): Boolean =
    configuration.getOptional[Boolean](s"controllers.$controllerName.needsAuditing").getOrElse(true)

    override def dataEvent(eventType: String, transactionName: String, request: RequestHeader, detail: Map[String, String])(implicit hc: HeaderCarrier): DataEvent = ???
}