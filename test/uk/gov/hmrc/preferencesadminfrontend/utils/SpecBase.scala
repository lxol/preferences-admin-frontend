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

package uk.gov.hmrc.preferencesadminfrontend.utils

import org.mockito.ArgumentMatcher
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Configuration
import play.api.inject.Injector
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.MergedDataEvent
import uk.gov.hmrc.play.config.AppName
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.preferencesadminfrontend.config.{AppConfig, FrontendAppConfig}
import uk.gov.hmrc.preferencesadminfrontend.connectors.{EntityResolverConnector, MessageConnector, PreferencesConnector}
import uk.gov.hmrc.preferencesadminfrontend.controllers.model.User
import uk.gov.hmrc.preferencesadminfrontend.services.RescindmentService

import scala.concurrent.Future

trait SpecBase extends UnitSpec with GuiceOneAppPerSuite with MockitoSugar {

  lazy val injector: Injector = app.injector
  implicit val appConfig: AppConfig = injector.instanceOf[FrontendAppConfig]
  implicit val user = User("me", "mySecretPassword")

  when(appConfig.analyticsToken).thenReturn("")
  when(appConfig.analyticsHost).thenReturn("")

  val auditConnectorMock = mock[AuditConnector]
  val entityResolverConnectorMock = mock[EntityResolverConnector]
  val preferencesConnectorMock = mock[PreferencesConnector]
  val messageConnectorMock = mock[MessageConnector]

  val appName = new AppName {
    protected def appNameConfiguration: Configuration = ???

    override def appName: String = "preferences-admin-frontend"
  }

  def isSimilar(expected: MergedDataEvent): ArgumentMatcher[MergedDataEvent] = {
    new ArgumentMatcher[MergedDataEvent]() {
      def matches(t: MergedDataEvent): Boolean = {
        t.auditSource == expected.auditSource &&
          t.auditType == expected.auditType &&
          t.request.tags == expected.request.tags &&
          t.request.detail == expected.request.detail &&
          t.response.tags == expected.response.tags &&
          t.response.detail == expected.response.detail
      }
    }
  }
}