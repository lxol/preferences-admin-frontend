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

import play.api.{Application, Logger}
import uk.gov.hmrc.crypto.ApplicationCrypto
import uk.gov.hmrc.preferencesadminfrontend.services.LoginServiceConfiguration

@Singleton
class FrontendStartup @Inject()(app: Application, appCrypto: ApplicationCrypto, graphiteConfiguration: GraphiteConfiguration, loginServiceConfiguration: LoginServiceConfiguration) {

  lazy val appName: String = app.configuration.getString("appName").getOrElse("APP NAME NOT SET")
  lazy val appMode = app.mode

  if (graphiteConfiguration.enabled) graphiteConfiguration.startGraphite()

  Logger.info(s"Starting frontend : $appName. : in mode : $appMode")
  appCrypto.verifyConfiguration()
  loginServiceConfiguration.verifyConfiguration()
}