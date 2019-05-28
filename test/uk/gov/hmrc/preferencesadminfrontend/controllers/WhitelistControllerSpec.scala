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

package uk.gov.hmrc.preferencesadminfrontend.controllers

import akka.stream.Materializer
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito.when
import org.scalatest.{Matchers, WordSpec}
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status
import play.api.i18n.MessagesApi
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsJson}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.mvc.Http
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.preferencesadminfrontend.connectors.MessageConnector
import uk.gov.hmrc.preferencesadminfrontend.controllers.model.User
import uk.gov.hmrc.preferencesadminfrontend.model.WhitelistEntry
import uk.gov.hmrc.preferencesadminfrontend.utils.{CSRFTest, SpecBase}

import scala.concurrent.Future

class WhitelistControllerSpec extends WordSpec with Matchers with MockitoSugar with GuiceOneAppPerSuite with SpecBase with CSRFTest {

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  implicit lazy val materializer: Materializer = app.materializer

  implicit lazy val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  implicit val hc: HeaderCarrier = mock[HeaderCarrier]

  "showWhitelistPage" should {

    "return 200 (Ok) when a populated whitelist is successfully retrieved from the message service" in new WhitelistControllerTestCase {
      private val fakeRequestWithSession = FakeRequest(routes.WhitelistController.showWhitelistPage()).withSession(User.sessionKey -> "user")
      private val whitelistJson = Json.parse(
        """
          |{
          | "formIdList" : ["SA359 2018","SA251 2018","SA370 2018"]
          |}
        """.stripMargin)
      when(mockMessageConnector.getWhitelist()(any[HeaderCarrier])).thenReturn(
        Future.successful(
          HttpResponse(Http.Status.OK,Some(whitelistJson))
        )
      )
      private val result = whitelistController.showWhitelistPage()(addToken(fakeRequestWithSession))
      status(result) shouldBe Status.OK
    }

    "return 200 (Ok) when an empty whitelist is successfully retrieved from the message service" in new WhitelistControllerTestCase {
      private val fakeRequestWithSession = FakeRequest(routes.WhitelistController.showWhitelistPage()).withSession(User.sessionKey -> "user")
      private val whitelistJson = Json.parse(
        """
          |{
          | "formIdList" : []
          |}
        """.stripMargin)
      when(mockMessageConnector.getWhitelist()(any[HeaderCarrier])).thenReturn(
        Future.successful(
          HttpResponse(Http.Status.OK,Some(whitelistJson))
        )
      )
      private val result = whitelistController.showWhitelistPage()(addToken(fakeRequestWithSession))
      status(result) shouldBe Status.OK
    }

    "return 502 (Bad Gateway) when the message service returns an invalid whitelist" in new WhitelistControllerTestCase {
      private val fakeRequestWithSession = FakeRequest(routes.WhitelistController.showWhitelistPage()).withSession(User.sessionKey -> "user")
      private val whitelistJson = Json.parse(
        """
          |{
          | "blah" : "blah"
          |}
        """.stripMargin)
      when(mockMessageConnector.getWhitelist()(any[HeaderCarrier])).thenReturn(
        Future.successful(
          HttpResponse(Http.Status.OK,Some(whitelistJson))
        )
      )
      private val result = whitelistController.showWhitelistPage()(addToken(fakeRequestWithSession))
      status(result) shouldBe Status.BAD_GATEWAY
    }

    "return 502 (Bad Gateway) when the message service returns any other status" in new WhitelistControllerTestCase {
      private val fakeRequestWithForm = FakeRequest(routes.WhitelistController.showWhitelistPage()).withSession(User.sessionKey -> "user")
      when(mockMessageConnector.getWhitelist()(any[HeaderCarrier])).thenReturn(
        Future.successful(
          HttpResponse(Http.Status.NOT_FOUND)
        )
      )
      private val result = whitelistController.showWhitelistPage()(addToken(fakeRequestWithForm))
      status(result) shouldBe Status.BAD_GATEWAY
    }

  }

  "confirmAdd" should {

    "return 303 (Redirect) when a Form ID is successfully added via the message service" in new WhitelistControllerTestCase {
      private val fakeRequestWithSession = FakeRequest(routes.WhitelistController.showWhitelistPage()).withSession(User.sessionKey -> "user")
      private val fakeRequestWithBody: FakeRequest[AnyContentAsJson] = fakeRequestWithSession.withJsonBody(
        Json.parse(
          """
            |{
            | "formId" : "SA316 2015",
            | "reasonText" : "some reason text"
            |}
          """.stripMargin)
      )
      when(mockMessageConnector.addFormIdToWhitelist(any[WhitelistEntry])(any[HeaderCarrier])).thenReturn(
        Future.successful(
          HttpResponse(Http.Status.CREATED)
        )
      )
      private val result = whitelistController.confirmAdd()(addToken(fakeRequestWithBody))
      status(result) shouldBe Status.SEE_OTHER
    }

    "return 502 (Bad Gateway) when the message service returns any other status" in new WhitelistControllerTestCase {
      private val fakeRequestWithSession = FakeRequest(routes.WhitelistController.showWhitelistPage()).withSession(User.sessionKey -> "user")
      private val fakeRequestWithBody: FakeRequest[AnyContentAsJson] = fakeRequestWithSession.withJsonBody(
        Json.parse(
          """
            |{
            | "formId" : "SA316 2015",
            | "reasonText" : "some reason text"
            |}
          """.stripMargin)
      )
      when(mockMessageConnector.addFormIdToWhitelist(any[WhitelistEntry])(any[HeaderCarrier])).thenReturn(
        Future.successful(
          HttpResponse(Http.Status.NOT_FOUND)
        )
      )
      private val result = whitelistController.confirmAdd()(addToken(fakeRequestWithBody))
      status(result) shouldBe Status.BAD_GATEWAY
    }

    "return 400 (Bad Request) when the Form ID JSON is not valid" in new WhitelistControllerTestCase {
      private val fakeRequestWithSession = FakeRequest(routes.WhitelistController.showWhitelistPage()).withSession(User.sessionKey -> "user")
      val fakeRequestWithBody: FakeRequest[AnyContentAsJson] = fakeRequestWithSession.withJsonBody(
        Json.parse(
          """
            |{
            | "blah" : "blah"
            |}
          """.stripMargin)
      )
      private val result = whitelistController.confirmAdd()(addToken(fakeRequestWithBody))
      status(result) shouldBe Status.BAD_REQUEST
    }
  }

  "confirmDelete" should {

    "return 303 (Redirect) when a Form ID is successfully deleted via the message service" in new WhitelistControllerTestCase {
      private val fakeRequestWithSession = FakeRequest(routes.WhitelistController.showWhitelistPage()).withSession(User.sessionKey -> "user")
      private val fakeRequestWithBody: FakeRequest[AnyContentAsJson] = fakeRequestWithSession.withJsonBody(
        Json.parse(
          """
            |{
            | "formId" : "SA316 2015",
            | "reasonText" : "some reason text"
            |}
          """.stripMargin)
      )
      when(mockMessageConnector.deleteFormIdFromWhitelist(any[WhitelistEntry])(any[HeaderCarrier])).thenReturn(
        Future.successful(
          HttpResponse(Http.Status.OK)
        )
      )
      private val result = whitelistController.confirmDelete()(addToken(fakeRequestWithBody))
      status(result) shouldBe Status.SEE_OTHER
    }

    "return 502 (Bad Gateway) when the message service returns any other status" in new WhitelistControllerTestCase {
      private val fakeRequestWithSession = FakeRequest(routes.WhitelistController.showWhitelistPage()).withSession(User.sessionKey -> "user")
      private val fakeRequestWithBody: FakeRequest[AnyContentAsJson] = fakeRequestWithSession.withJsonBody(
        Json.parse(
          """
            |{
            | "formId" : "SA316 2015",
            | "reasonText" : "some reason text"
            |}
          """.stripMargin)
      )
      when(mockMessageConnector.deleteFormIdFromWhitelist(any[WhitelistEntry])(any[HeaderCarrier])).thenReturn(
        Future.successful(
          HttpResponse(Http.Status.NOT_FOUND)
        )
      )
      private val result = whitelistController.confirmDelete()(addToken(fakeRequestWithBody))
      status(result) shouldBe Status.BAD_GATEWAY
    }

    "return 400 (Bad Request) when the form ID JSON is not valid" in new WhitelistControllerTestCase {
      private val fakeRequestWithSession = FakeRequest(routes.WhitelistController.showWhitelistPage()).withSession(User.sessionKey -> "user")
      val fakeRequestWithBody: FakeRequest[AnyContentAsJson] = fakeRequestWithSession.withJsonBody(
        Json.parse(
          """
            |{
            | "blah" : "blah"
            |}
          """.stripMargin)
      )
      private val result = whitelistController.confirmDelete()(addToken(fakeRequestWithBody))
      status(result) shouldBe Status.BAD_REQUEST
    }
  }

}

trait WhitelistControllerTestCase extends SpecBase with MockitoSugar {

  val mockMessageConnector: MessageConnector = mock[MessageConnector]

  def whitelistController()(implicit messages: MessagesApi):WhitelistController = new WhitelistController(mockMessageConnector)

}
