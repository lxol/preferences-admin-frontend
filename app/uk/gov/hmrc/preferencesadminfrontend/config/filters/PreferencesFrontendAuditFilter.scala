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

import akka.stream.Materializer
import javax.inject.Inject
import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.audit.DefaultAuditConnector
import uk.gov.hmrc.play.bootstrap.filters.frontend.{DefaultFrontendAuditFilter, FrontendAuditFilter}
import uk.gov.hmrc.play.config.{AppName, RunMode}

class PreferencesFrontendAuditFilter @Inject()(configuration: Configuration, name: AppName,
                                               runMode: RunMode,
                                               frontendAuditConnector: DefaultAuditConnector)(implicit val mat: Materializer) extends DefaultFrontendAuditFilter {

  override val maskedFormFields = Seq("password")
  lazy val auditConnector = frontendAuditConnector

  def controllerNeedsAuditing(controllerName: String): Boolean =
    configuration.getBoolean(s"controllers.$controllerName.needsAuditing").getOrElse(true)

  override def appName: String = name.appName
}