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

package uk.gov.hmrc.preferencesadminfrontend

import akka.actor.ActorSystem
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.{Configuration, Environment}
import play.api.http.Status
import play.api.libs.json._
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.preferencesadminfrontend.connectors.MessageConnector

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.preferencesadminfrontend.model.{RescindmentAlertsResult, RescindmentRequest, RescindmentUpdateResult}

class MessageConnectorSpec extends UnitSpec with ScalaFutures with GuiceOneAppPerSuite {

  "addRescindments" should {
    "return a valid update result with status 200" in new TestCase {
      val result = messageConnectorPostMock(expectedAddRescindmentsPath, rescindmentUpdateResultJson, Status.OK)
        .addRescindments(rescindmentRequest).futureValue
      result shouldBe rescindmentUpdateResult
    }
  }

  "sendRescindmentAlerts" should {
    "return a valid alert result with status 200" in new TestCase {
      val result = messageConnectorPostMock(expectedSendRescindmentAlertsPath, rescindmentAlertsResultJson, Status.OK)
        .sendRescindmentAlerts().futureValue
      result shouldBe rescindmentAlertsResult
    }
  }

  trait TestCase extends MockitoSugar {
    implicit val hc: HeaderCarrier = HeaderCarrier()

    lazy val environment = app.injector.instanceOf[Environment]
    lazy val configuration = app.injector.instanceOf[Configuration]
    lazy val frontendAuditConnector = app.injector.instanceOf[FrontendAuditConnector]
    lazy val actorSystem = app.injector.instanceOf[ActorSystem]
    lazy val mockResponse = mock[HttpResponse]

    val expectedAddRescindmentsPath = s"/admin/message/add-rescindments"
    val expectedSendRescindmentAlertsPath = s"/admin/send-rescindment-alerts"
    val emptyJson = Json.obj()
    val rescindmentRequest = RescindmentRequest(
      batchId = "1234567",
      formId = "SA316",
      date = "2017-03-16",
      reference = "ref-test",
      emailTemplateId = "rescindedMessageAlert"
    )
    val rescindmentUpdateResultJson = Json.obj(
      "tried" -> 1,
      "succeeded" -> 1,
      "alreadyUpdated" -> 0,
      "invalidState" -> 0
    )
    val rescindmentUpdateResult = RescindmentUpdateResult(
      tried = 1, succeeded = 1, alreadyUpdated = 0, invalidState = 0
    )
    val rescindmentAlertsResultJson = Json.obj(
      "alerts sent" -> 1,
      "ready for retrial" -> 1,
      "failed permanently" -> 0,
      "hard copy requested" -> 0
    )
    val rescindmentAlertsResult = RescindmentAlertsResult(
      sent = 1, requeued = 1, failed = 0, hardCopyRequested = 0
    )

    def messageConnectorPostMock(expectedPath: String, jsonBody: JsValue, status: Int): MessageConnector = {
      new MessageConnector(frontendAuditConnector, environment, configuration, actorSystem) {
        override def doPost[A](url: String, body: A, headers: Seq[(String, String)])
                              (implicit rds: Writes[A], hc: HeaderCarrier): Future[HttpResponse] = {
          url should include(expectedPath)
          when(mockResponse.json).thenReturn(jsonBody)
          when(mockResponse.status).thenReturn(status)
          Future.successful(mockResponse)
        }

        override def doEmptyPost[A](url: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
          url should include(expectedPath)
          when(mockResponse.json).thenReturn(jsonBody)
          when(mockResponse.status).thenReturn(status)
          Future.successful(mockResponse)
        }
      }
    }

    def messageConnectorPostMock(expectedPath: String, error: Throwable): MessageConnector = {
      new MessageConnector(frontendAuditConnector, environment, configuration, actorSystem) {
        override def doPost[A](url: String, body: A, headers: Seq[(String, String)])
                              (implicit rds: Writes[A], hc: HeaderCarrier): Future[HttpResponse] = {
          url should include(expectedPath)
          Future.failed(error)
        }

        override def doEmptyPost[A](url: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
          url should include(expectedPath)
          Future.failed(error)
        }
      }
    }
  }
}
