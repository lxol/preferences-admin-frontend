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
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers._
import org.scalatest.Matchers
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status
import play.api.i18n.MessagesApi
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsFormUrlEncoded, Call}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.preferencesadminfrontend.connectors.MessageConnector
import uk.gov.hmrc.preferencesadminfrontend.controllers.model.User
import uk.gov.hmrc.preferencesadminfrontend.model.{BatchMessagePreview, GmcBatch, MessagePreview}
import uk.gov.hmrc.preferencesadminfrontend.services.MessageService
import uk.gov.hmrc.preferencesadminfrontend.utils.{CSRFTest, SpecBase}

import scala.concurrent.{ExecutionContext, Future}

class MessageBrakeControllerSpec extends UnitSpec
  with Matchers
  with MockitoSugar
  with GuiceOneAppPerSuite
  with SpecBase
  with CSRFTest
  with ScalaFutures {

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  implicit lazy val materializer: Materializer = app.materializer

  implicit lazy val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  implicit val hc: HeaderCarrier = mock[HeaderCarrier]

  implicit val ec = ExecutionContext.global

  val gmcBatch = GmcBatch(
    "123456789",
    "SA359",
    "2017-03-16",
    "newMessageAlert_SA359",
    Some(15778)
  )

  val mockMessagePreview = MessagePreview(
    "subject",
    "content",
    "123456789",
    "messageType",
    "03/04/1995",
    "AB123456C"
  )

  val mockedBatchMessagePreview = BatchMessagePreview(
    mockMessagePreview,
    "123456789"
  )

  "showAdminPage" should {

    "return a 200 when the admin page is successfully populated with Gmc Message Batches" in new MessageBrakeControllerTestCase {
      private val fakeRequestWithSession = FakeRequest(routes.MessageBrakeController.showAdminPage()).withSession(User.sessionKey -> "user")
      when(mockMessageService.getGmcBatches()(any[HeaderCarrier], any[ExecutionContext])).thenReturn (Future.successful(Left(Seq(gmcBatch))))
      private val result = messageBrakeController.showAdminPage()(addToken(fakeRequestWithSession))
      status(result) shouldBe Status.OK
    }

    "return error" in new MessageBrakeControllerTestCase {
      private val fakeRequestWithSession = FakeRequest(routes.MessageBrakeController.showAdminPage()).withSession(User.sessionKey -> "user")
      when(mockMessageService.getGmcBatches()(any[HeaderCarrier], any[ExecutionContext])).thenReturn (Future.successful(Right("error")))
      private val result = messageBrakeController().showAdminPage()(addToken(fakeRequestWithSession))
      status(result) shouldBe Status.BAD_GATEWAY
    }
  }

  "previewMessage" should {

    "return 200 when the preview page has been populated with a single message" in new MessageBrakeControllerTestCase {
      val requestWithFormData = getRequestWithFormData(routes.MessageBrakeController.previewMessage())
      when(mockMessageService.getGmcBatches()(any[HeaderCarrier],any[ExecutionContext])).thenReturn(Future.successful(Left(Seq(gmcBatch))))
      when(mockMessageService.getRandomMessagePreview(ArgumentMatchers.eq(gmcBatch))(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future(Left(mockedBatchMessagePreview)))
      private val result = messageBrakeController().previewMessage()(addToken(requestWithFormData))
      status(result) shouldBe Status.OK
    }

    "return error when getGmcBatches() fails" in new MessageBrakeControllerTestCase {
      val requestWithFormData = getRequestWithFormData(routes.MessageBrakeController.previewMessage())
      when(mockMessageService.getGmcBatches()(any[HeaderCarrier],any[ExecutionContext])).thenReturn(Future.successful(Right("error")))
      when(mockMessageService.getRandomMessagePreview(ArgumentMatchers.eq(gmcBatch))(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future(Left(mockedBatchMessagePreview)))
      private val result = messageBrakeController().previewMessage()(addToken(requestWithFormData))
      status(result) shouldBe Status.BAD_GATEWAY
    }

    "return error when getRandomMessagePreview() fails" in new MessageBrakeControllerTestCase {
      val requestWithFormData = getRequestWithFormData(routes.MessageBrakeController.previewMessage())
      when(mockMessageService.getGmcBatches()(any[HeaderCarrier],any[ExecutionContext])).thenReturn(Future.successful(Left(Seq(gmcBatch))))
      when(mockMessageService.getRandomMessagePreview(ArgumentMatchers.eq(gmcBatch))(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future(Right("error")))
      private val result = messageBrakeController().previewMessage()(addToken(requestWithFormData))
      status(result) shouldBe Status.BAD_GATEWAY
    }
  }

  "approveBatch" should {

    "return 200 when the approve batch page is posted" in new MessageBrakeControllerTestCase {
      val requestWithFormData = getRequestWithFormData(routes.MessageBrakeController.approveBatch())
      when(mockMessageConnector.approveGmcBatch(ArgumentMatchers.eq(gmcBatch))(any[HeaderCarrier]))
        .thenReturn(Future(HttpResponse(Status.OK)))
      when(mockMessageService.getGmcBatches()(any[HeaderCarrier],any[ExecutionContext])).thenReturn(Future.successful(Left(Seq(gmcBatch))))
      private val result = messageBrakeController().approveBatch()(addToken(requestWithFormData))
      status(result) shouldBe Status.OK
    }

    "return error when approveGmcBatch() fails" in new MessageBrakeControllerTestCase {
      val requestWithFormData = getRequestWithFormData(routes.MessageBrakeController.approveBatch())
      when(mockMessageConnector.approveGmcBatch(ArgumentMatchers.eq(gmcBatch))(any[HeaderCarrier]))
        .thenReturn(Future(HttpResponse(Status.BAD_GATEWAY)))
      when(mockMessageService.getGmcBatches()(any[HeaderCarrier],any[ExecutionContext])).thenReturn(Future.successful(Left(Seq(gmcBatch))))
      private val result = messageBrakeController().approveBatch()(addToken(requestWithFormData))
      status(result) shouldBe Status.BAD_GATEWAY
    }

    "return error when getGmcBatches() fails" in new MessageBrakeControllerTestCase {
      val requestWithFormData = getRequestWithFormData(routes.MessageBrakeController.approveBatch())
      when(mockMessageConnector.approveGmcBatch(ArgumentMatchers.eq(gmcBatch))(any[HeaderCarrier]))
        .thenReturn(Future(HttpResponse(Status.OK)))
      when(mockMessageService.getGmcBatches()(any[HeaderCarrier],any[ExecutionContext])).thenReturn(Future.successful(Right("error")))
      private val result = messageBrakeController().approveBatch()(addToken(requestWithFormData))
      status(result) shouldBe Status.BAD_GATEWAY
    }
  }

  "rejectBatch" should {

    "return 200 when the approve batch page is posted" in new MessageBrakeControllerTestCase {
      val requestWithFormData = getRequestWithFormData(routes.MessageBrakeController.rejectBatch())
      when(mockMessageConnector.rejectGmcBatch(ArgumentMatchers.eq(gmcBatch))(any[HeaderCarrier]))
        .thenReturn(Future(HttpResponse(Status.OK)))
      when(mockMessageService.getGmcBatches()(any[HeaderCarrier],any[ExecutionContext])).thenReturn(Future.successful(Left(Seq(gmcBatch))))
      private val result = messageBrakeController().rejectBatch()(addToken(requestWithFormData))
      status(result) shouldBe Status.OK
    }

    "return error when rejectGmcBatch() fails" in new MessageBrakeControllerTestCase {
      val requestWithFormData = getRequestWithFormData(routes.MessageBrakeController.rejectBatch())
      when(mockMessageConnector.rejectGmcBatch(ArgumentMatchers.eq(gmcBatch))(any[HeaderCarrier]))
        .thenReturn(Future(HttpResponse(Status.BAD_GATEWAY)))
      when(mockMessageService.getGmcBatches()(any[HeaderCarrier],any[ExecutionContext])).thenReturn(Future.successful(Left(Seq(gmcBatch))))
      private val result = messageBrakeController().rejectBatch()(addToken(requestWithFormData))
      status(result) shouldBe Status.BAD_GATEWAY
    }

    "return error when getGmcBatches() fails" in new MessageBrakeControllerTestCase {
      val requestWithFormData = getRequestWithFormData(routes.MessageBrakeController.rejectBatch())
      when(mockMessageConnector.rejectGmcBatch(ArgumentMatchers.eq(gmcBatch))(any[HeaderCarrier]))
        .thenReturn(Future(HttpResponse(Status.OK)))
      when(mockMessageService.getGmcBatches()(any[HeaderCarrier],any[ExecutionContext])).thenReturn(Future.successful(Right("error")))
      private val result = messageBrakeController().rejectBatch()(addToken(requestWithFormData))
      status(result) shouldBe Status.BAD_GATEWAY
    }
  }

}

trait MessageBrakeControllerTestCase extends SpecBase with MockitoSugar {

  val mockMessageConnector: MessageConnector = mock[MessageConnector]

  val mockMessageService: MessageService = mock[MessageService]

  def messageBrakeController()(implicit messages: MessagesApi):MessageBrakeController = new MessageBrakeController(mockMessageConnector, mockMessageService)

  def getRequestWithFormData(routeCall: Call): FakeRequest[AnyContentAsFormUrlEncoded] = {
    val fakeRequestWithSession = FakeRequest(routeCall).withSession(User.sessionKey -> "user")
    fakeRequestWithSession.withFormUrlEncodedBody(
      "batchId" -> "123456789",
      "formId" -> "SA359",
      "issueDate" -> "2017-03-16",
      "templateId" -> "newMessageAlert_SA359",
      "count" -> "15778"
    )
  }
}
