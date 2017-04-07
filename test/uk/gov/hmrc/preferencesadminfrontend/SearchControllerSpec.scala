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

package uk.gov.hmrc.preferencesadminfrontend

import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Configuration
import play.api.http.Status
import play.api.i18n.MessagesApi
import play.api.test.FakeRequest
import play.api.test.Helpers.{headers, _}
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.preferencesadminfrontend.config.FrontendAppConfig
import uk.gov.hmrc.preferencesadminfrontend.controllers.SearchController
import uk.gov.hmrc.preferencesadminfrontend.controllers.model.User
import uk.gov.hmrc.preferencesadminfrontend.utils.CSRFTest


class SearchControllerSpec extends SearchControllerCase  with CSRFTest with ScalaFutures{

  "SearchController" should {

    "return ok if session is authorised" in {
      val result = searchController.showSearchPage(addToken(FakeRequest().withSession(User.sessionKey -> "user")))

      status(result) shouldBe Status.OK
    }

    "redirect to login page if not authorised" in {
      val result = searchController.showSearchPage(addToken(FakeRequest().withSession()))

      status(result) shouldBe Status.SEE_OTHER
      headers(result) should contain("Location" -> "/paperless/admin")
    }
  }
}


trait SearchControllerCase extends UnitSpec with GuiceOneAppPerSuite with MockitoSugar{

  implicit val appConfig = mock[FrontendAppConfig]
  implicit val messagesApi = app.injector.instanceOf[MessagesApi]

  when(appConfig.analyticsToken).thenReturn("")
  when(appConfig.analyticsHost).thenReturn("")

  val playConfiguration = app.injector.instanceOf[Configuration]

  val searchController = new SearchController()
}
