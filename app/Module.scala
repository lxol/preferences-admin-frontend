/*
 * Copyright 2018 HM Revenue & Customs
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

import com.google.inject.AbstractModule
import play.api.{Logger, LoggerLike}
import uk.gov.hmrc.crypto.{ApplicationCrypto, ApplicationCryptoDI}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.config.inject.{DefaultRunMode, RunMode}
import uk.gov.hmrc.play.frontend.bootstrap.FrontendFilters
import uk.gov.hmrc.preferencesadminfrontend.FrontendAuditConnector
import uk.gov.hmrc.preferencesadminfrontend.config._
import uk.gov.hmrc.preferencesadminfrontend.config.filters.PreferencesFrontendLoggingFilter
import uk.gov.hmrc.play.frontend.filters.FrontendLoggingFilter

class Module extends AbstractModule {

  override def configure(): Unit = {

    // On startup
    bind(classOf[FrontendStartup]).asEagerSingleton()
    bind(classOf[AppConfig]).to(classOf[FrontendAppConfig]).asEagerSingleton()
    bind(classOf[FrontendFilters]).to(classOf[AdminFrontendGlobal]).asEagerSingleton()

    bind(classOf[RunMode]).to(classOf[DefaultRunMode])
    bind(classOf[ApplicationCrypto]).to(classOf[ApplicationCryptoDI])
    bind(classOf[FrontendLoggingFilter]).to(classOf[PreferencesFrontendLoggingFilter])
    bind(classOf[LoggerLike]) toInstance Logger

    bind(classOf[AuditConnector]).to(classOf[FrontendAuditConnector])
  }

}