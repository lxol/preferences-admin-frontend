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
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse}
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

      val result = createEntityConnector(expectedPath, responseJson, Status.OK).getTaxIdentifiers(sautr).futureValue

      result.size shouldBe (1)
      result should contain(sautr)
    }

    "return all tax identifiers for sautr" in new TestCase {
      val expectedPath = s"/entity-resolver/sa/${sautr.value}"
      val responseJson = taxIdentifiersResponseFor(sautr, nino)

      val result = createEntityConnector(expectedPath, responseJson, Status.OK).getTaxIdentifiers(sautr).futureValue

      result.size shouldBe (2)
      result should contain(nino)
      result should contain(sautr)
    }

    "return all tax identifiers for nino" in new TestCase {
      val expectedPath = s"/entity-resolver/paye/${nino.value}"
      val responseJson = taxIdentifiersResponseFor(sautr, nino)

      val result = createEntityConnector(expectedPath, responseJson, Status.OK).getTaxIdentifiers(nino).futureValue

      result.size shouldBe (2)
      result should contain(nino)
      result should contain(sautr)
    }

    "return empty sequence" in new TestCase {
      val expectedPath = s"/entity-resolver/paye/${nino.value}"

      val result = createEntityConnector(expectedPath, emptyJson, Status.NOT_FOUND).getTaxIdentifiers(nino).futureValue

      result.size shouldBe (0)
    }

  }

  "getPreferenceDetails" should {

    "return details if sautr exists" in new TestCase {
      val expectedPath = s"/portal/preferences/sa/${sautr.value}"
      val responseJson = preferenceDetailsResponseFor(true, true)

      val result = createEntityConnector(expectedPath, responseJson, Status.OK).getPreferenceDetails(sautr).futureValue

      result shouldBe defined
      result.get.paperless shouldBe true
      result.get.email shouldBe Email("john.doe@digital.hmrc.gov.uk", true)
    }

    "return details if nino exists" in new TestCase {
      val expectedPath = s"/portal/preferences/paye/${nino.value}"
      val responseJson = preferenceDetailsResponseFor(true, true)

      val result = createEntityConnector(expectedPath, responseJson, Status.OK).getPreferenceDetails(nino).futureValue

      result shouldBe defined
      result.get.paperless shouldBe true
      result.get.email shouldBe Email("john.doe@digital.hmrc.gov.uk", true)
    }

    "return None if taxId does not exist" in new TestCase {
      val expectedPath = s"/portal/preferences/sa/${sautr.value}"

      val result = createEntityConnector(expectedPath, emptyJson, Status.NOT_FOUND).getPreferenceDetails(sautr).futureValue

      result should not be defined
    }
  }

  trait TestCase extends MockitoSugar {
    val sautr = TaxIdentifier("sautr", Random.nextInt(1000000).toString)
    val nino = TaxIdentifier("nino", "NA000914D")

    implicit val hc: HeaderCarrier = HeaderCarrier()

    lazy val serviceConfig = app.injector.instanceOf[ServicesConfig]
    lazy val mockResponse = mock[HttpResponse]
    val emptyJson = Json.obj()


    def createEntityConnector(expectedPath: String, jsonBody: JsValue, status: Int): EntityResolverConnector = {
      new EntityResolverConnector(serviceConfig) {
        override def doGet(url: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
          url should include(expectedPath)
          setupResponseFor(jsonBody, status)
          Future.successful(mockResponse)
        }
      }
    }

    lazy val entityResolverConnector = new EntityResolverConnector(serviceConfig) {
      override def doGet(url: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = mockResponse
    }

    def setupResponseFor(jsonBody: JsValue, status: Int): Unit = {
      when(mockResponse.json).thenReturn(jsonBody)
      when(mockResponse.body).thenReturn(jsonBody.toString())
      when(mockResponse.status).thenReturn(status)
    }

    def taxIdentifiersResponseFor(taxIds: TaxIdentifier*) = {
      val taxIdsJson: Seq[(String, JsValue)] = taxIds.map {
        case TaxIdentifier(name, value) => name -> JsString(value)
      }
      taxIdsJson.foldLeft(Json.obj("_id" -> "6a048719-3d4b-4a3e-9440-17b238807bc9"))(_ + _)
    }

    def preferenceDetailsResponseFor(paperless: Boolean, emailVerified: Boolean) = {
      Json.parse(
        s"""
           |{
           |  "digital": $paperless,
           |  "email": {
           |    "email": "john.doe@digital.hmrc.gov.uk",
           |    "status": "${if (emailVerified) "verified" else ""}",
           |    "mailboxFull": false
           |  }
           |}
       """.stripMargin)
    }

  }

}
