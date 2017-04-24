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

import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status
import play.api.libs.json._
import uk.gov.hmrc.play.config.inject.ServicesConfig
import uk.gov.hmrc.play.http._
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.preferencesadminfrontend.connectors.EntityResolverConnector
import uk.gov.hmrc.preferencesadminfrontend.services.model.{Email, TaxIdentifier}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Random

class EntityResolverConnectorSpec extends UnitSpec with ScalaFutures with GuiceOneAppPerSuite {

  "getTaxIdentifiers" should {
    "return only sautr if nino does not exist" in new TestCase {
      val expectedPath = s"/entity-resolver/sa/${sautr.value}"
      val responseJson = taxIdentifiersResponseFor(sautr)

      val result = entityConnectorGetMock(expectedPath, responseJson, Status.OK).getTaxIdentifiers(sautr).futureValue

      result.size shouldBe (1)
      result should contain(sautr)
    }

    "return all tax identifiers for sautr" in new TestCase {
      val expectedPath = s"/entity-resolver/sa/${sautr.value}"
      val responseJson = taxIdentifiersResponseFor(sautr, nino)

      val result = entityConnectorGetMock(expectedPath, responseJson, Status.OK).getTaxIdentifiers(sautr).futureValue

      result.size shouldBe (2)
      result should contain(nino)
      result should contain(sautr)
    }

    "return all tax identifiers for nino" in new TestCase {
      val expectedPath = s"/entity-resolver/paye/${nino.value}"
      val responseJson = taxIdentifiersResponseFor(sautr, nino)

      val result = entityConnectorGetMock(expectedPath, responseJson, Status.OK).getTaxIdentifiers(nino).futureValue

      result.size shouldBe (2)
      result should contain(nino)
      result should contain(sautr)
    }

    "return empty sequence" in new TestCase {
      val expectedPath = s"/entity-resolver/paye/${nino.value}"

      val result = entityConnectorGetMock(expectedPath, emptyJson, Status.NOT_FOUND).getTaxIdentifiers(nino).futureValue

      result.size shouldBe (0)
    }

    "return empty sequence  if Entity-Resolver cannot parse parameter" in new TestCase {
      val expectedPath = s"/entity-resolver/paye/${nino.value}"
      val error = new BadRequestException(message=s"""'{"statusCode":400,"message":"Cannot parse parameter '${nino.name}' with value '${nino.value}'"}'""")

      val result = entityConnectorGetMock(expectedPath, error).getTaxIdentifiers(nino).futureValue

      result shouldBe empty
    }
  }

  "getPreferenceDetails" should {

    "return paperless preference true and valid email address and verification true if user is opted in for saUtr" in new TestCase {
      val expectedPath = s"/portal/preferences/sa/${sautr.value}"
      val responseJson = preferenceDetailsResponseForOptedIn(true)

      val result = entityConnectorGetMock(expectedPath, responseJson, Status.OK).getPreferenceDetails(sautr).futureValue

      result shouldBe defined
      result.get.paperless shouldBe true
      result.get.email shouldBe Some(Email("john.doe@digital.hmrc.gov.uk", true))
    }

    "return paperless preference true and valid email address and verification false if user is opted in for saUtr" in new TestCase {
      val expectedPath = s"/portal/preferences/sa/${sautr.value}"
      val responseJson = preferenceDetailsResponseForOptedIn(false)

      val result = entityConnectorGetMock(expectedPath, responseJson, Status.OK).getPreferenceDetails(sautr).futureValue

      result shouldBe defined
      result.get.paperless shouldBe true
      result.get.email shouldBe Some(Email("john.doe@digital.hmrc.gov.uk", false))
    }

    "return paperless preference false and email as 'None' if user is opted out for saUtr" in new TestCase {
      val expectedPath = s"/portal/preferences/sa/${sautr.value}"
      val responseJson = preferenceDetailsResponseForOptedOut()

      val result = entityConnectorGetMock(expectedPath, responseJson, Status.OK).getPreferenceDetails(sautr).futureValue

      result shouldBe defined
      result.get.paperless shouldBe false
      result.get.email shouldBe None
    }

    "return email address and verification if user is opted in for nino" in new TestCase {
      val expectedPath = s"/portal/preferences/paye/${nino.value}"
      val responseJson = preferenceDetailsResponseForOptedIn(true)

      val result = entityConnectorGetMock(expectedPath, responseJson, Status.OK).getPreferenceDetails(nino).futureValue

      result shouldBe defined
      result.get.paperless shouldBe true
      result.get.email shouldBe Some(Email("john.doe@digital.hmrc.gov.uk", true))
    }

    "return None if taxId does not exist" in new TestCase {
      val expectedPath = s"/portal/preferences/sa/${sautr.value}"

      val result = entityConnectorGetMock(expectedPath, emptyJson, Status.NOT_FOUND).getPreferenceDetails(sautr).futureValue

      result should not be defined
    }

    "return None if taxId is malformed" in new TestCase {
      val expectedPath = s"/portal/preferences/paye/${nino.value}"
      val error = new BadRequestException(message=s"""'{"statusCode":400,"message":"Cannot parse parameter '${nino.name}' with value '${nino.value}'"}'""")

      val result = entityConnectorGetMock(expectedPath, error).getPreferenceDetails(nino).futureValue

      result should not be defined
    }
  }

  "optOut" should {
    "return true if status is OK (user is opted out)" in new TestCase {
      val expectedPath = s"/entity-resolver-admin/manual-opt-out/sa/${sautr.value}"

      val result = entityConnectorPostMock(expectedPath, emptyJson, Status.OK).optOut(sautr).futureValue

      result shouldBe true
    }

    "return false if CONFLICT" in new TestCase {
      val expectedPath = s"/entity-resolver-admin/manual-opt-out/sa/${sautr.value}"

      val result = entityConnectorPostMock(expectedPath, emptyJson, Status.CONFLICT).optOut(sautr).futureValue

      result shouldBe false
    }

    "return false if NOT_FOUND" in new TestCase {
      val expectedPath = s"/entity-resolver-admin/manual-opt-out/sa/${sautr.value}"

      val result = entityConnectorPostMock(expectedPath, emptyJson, Status.NOT_FOUND).optOut(sautr).futureValue

      result shouldBe false
    }

    "return false if PRECONDITION_FAILED" in new TestCase {
      val expectedPath = s"/entity-resolver-admin/manual-opt-out/sa/${sautr.value}"

      val result = entityConnectorPostMock(expectedPath, emptyJson, Status.PRECONDITION_FAILED).optOut(sautr).futureValue

      result shouldBe false
    }

  }

  trait TestCase extends MockitoSugar {
    val sautr = TaxIdentifier("sautr", Random.nextInt(1000000).toString)
    val nino = TaxIdentifier("nino", "NA000914D")

    implicit val hc: HeaderCarrier = HeaderCarrier()

    lazy val serviceConfig = app.injector.instanceOf[ServicesConfig]
    lazy val mockResponse = mock[HttpResponse]
    val emptyJson = Json.obj()


    def entityConnectorGetMock(expectedPath: String, jsonBody: JsValue, status: Int): EntityResolverConnector = {
      new EntityResolverConnector(serviceConfig) {
        override def doGet(url: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
          url should include(expectedPath)
          when(mockResponse.json).thenReturn(jsonBody)
          when(mockResponse.status).thenReturn(status)
          Future.successful(mockResponse)
        }
      }
    }

    def entityConnectorGetMock(expectedPath: String, error: Throwable): EntityResolverConnector = {
      new EntityResolverConnector(serviceConfig) {
        override def doGet(url: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
          url should include(expectedPath)
          Future.failed(error)
        }
      }
    }

    def entityConnectorPostMock(expectedPath: String, jsonBody: JsValue, status: Int): EntityResolverConnector = {
      new EntityResolverConnector(serviceConfig) {
        override def doEmptyPost[A](url: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
          url should include(expectedPath)
          when(mockResponse.json).thenReturn(jsonBody)
          when(mockResponse.status).thenReturn(status)
          Future.successful(mockResponse)
        }
      }
    }



    def taxIdentifiersResponseFor(taxIds: TaxIdentifier*) = {
      val taxIdsJson: Seq[(String, JsValue)] = taxIds.map {
        case TaxIdentifier(name, value) => name -> JsString(value)
      }
      taxIdsJson.foldLeft(Json.obj("_id" -> "6a048719-3d4b-4a3e-9440-17b238807bc9"))(_ + _)
    }

    def preferenceDetailsResponseForOptedIn(emailVerified: Boolean) = {
      Json.parse(
        s"""
           |{
           |  "digital": true,
           |  "email": {
           |    "email": "john.doe@digital.hmrc.gov.uk",
           |    "status": "${if (emailVerified) "verified" else ""}",
           |    "mailboxFull": false
           |  }
           |}
       """.stripMargin)
    }

    def preferenceDetailsResponseForOptedOut() = {
      Json.parse(
        s"""
           |{
           |  "digital": false
           |}
       """.stripMargin)
    }

  }

}
