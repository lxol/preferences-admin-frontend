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

import javax.inject.Inject

import akka.stream.Materializer
import play.api.Configuration
import uk.gov.hmrc.play.audit.filters.FrontendAuditFilter
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.preferencesadminfrontend.FrontendAuditConnector
import uk.gov.hmrc.preferencesadminfrontend.config.ControllerConfiguration

class MicroserviceAuditFilter @Inject()(
                                         conf: Configuration,
                                         controllerConf: ControllerConfiguration
                                       )(implicit val mat: Materializer) extends FrontendAuditFilter {

  override lazy val appName = conf.getString("appName").getOrElse("APP NAME NOT SET")
  override lazy val maskedFormFields = Seq("password")
  override lazy val applicationPort = None

  override def auditConnector: AuditConnector = FrontendAuditConnector

  override def controllerNeedsAuditing(controllerName: String): Boolean = {
    controllerConf.getControllerConfig(controllerName).needsAuditing
  }

}