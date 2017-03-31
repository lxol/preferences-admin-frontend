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

package uk.gov.hmrc.preferencesadminfrontend.controllers

import org.scalatest.Matchers._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Configuration
import play.api.http._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import org.mockito.Mockito._
import uk.gov.hmrc.preferencesadminfrontend.config.FrontendAppConfig
import uk.gov.hmrc.preferencesadminfrontend.controllers.model.User
import uk.gov.hmrc.preferencesadminfrontend.services.{LoginService, LoginServiceConfiguration}
import uk.gov.hmrc.preferencesadminfrontend.utils.CSRFTest

class LoginControllerSpec
  extends PlaySpec
    with GuiceOneAppPerSuite
    with ScalaFutures
    with LoginControllerFixtures
    with CSRFTest {

  "GET /" should {
    "return 200" in {
      val result = loginController.showLoginPage(addToken(FakeRequest("GET", "/")))
      status(result) shouldBe Status.OK
    }

    "return HTML" in {
      val result = loginController.showLoginPage(addToken(FakeRequest("GET", "/")))
      contentType(result) shouldBe Some("text/html")
      charset(result) shouldBe Some("utf-8")
    }
  }

  "POST to login" should {
    "Return the next page if credentials are correct" in {
      val result = loginController.login(
        addToken(FakeRequest()).withFormUrlEncodedBody(
          "username" -> "usernameTest",
          "password" -> "passwordTest"
        )
      )

      result.futureValue.header.status shouldBe Status.OK
    }

    "Return unauthorised if credentials are not correct" in {
      val result = loginController.login(
        addToken(FakeRequest()).withFormUrlEncodedBody(
          "username" -> "usernameTest",
          "password" -> "wrongPassword"
        )
      )

      result.futureValue.header.status shouldBe Status.UNAUTHORIZED
    }

    "Return bad request if credentials are missing" in {
      val result = loginController.login(
        addToken(FakeRequest()).withFormUrlEncodedBody()
      )

      result.futureValue.header.status shouldBe Status.BAD_REQUEST
    }
  }
}

trait LoginControllerFixtures extends MockitoSugar  {

  implicit val appConfig = mock[FrontendAppConfig]

  when(appConfig.analyticsToken).thenReturn("")
  when(appConfig.analyticsHost).thenReturn("")

  val loginServiceConfiguration = new LoginServiceConfiguration(mock[Configuration]){
    override lazy val authorisedUsers: Seq[User] = Seq(User("usernameTest", "passwordTest"))
  }
  val loginController = new LoginController(new LoginService(loginServiceConfiguration))
}
