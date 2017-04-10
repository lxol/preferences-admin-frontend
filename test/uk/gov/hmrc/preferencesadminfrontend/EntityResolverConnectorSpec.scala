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
import play.api.http.Status
import play.api.libs.json._
import play.api.libs.ws.{WSClient, WSRequest, WSResponse}
import uk.gov.hmrc.play.config.inject.ServicesConfig
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.preferencesadminfrontend.connectors.EntityResolverConnector
import uk.gov.hmrc.preferencesadminfrontend.services.model.TaxIdentifier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Random

class EntityResolverConnectorSpec extends UnitSpec with ScalaFutures {


  "getTaxIdentifiers" should {
    "return only sautr if nino does not exist" in new TestCase {
      val expectedPath = s"/entity-resolver/sa/${sautr.value}"
      val responseJson = responseJsonFor(sautr)
      val response = wsResponseFor(responseJson, Status.OK)

      when(wsClient.url(expectedPath)).thenReturn(mockRequest)
      when(mockRequest.get()).thenReturn(Future.successful(response))


      val result = entityResolverConnector.getTaxIdentifiers(sautr).futureValue

      result.size shouldBe(1)
      result should contain (sautr)
    }


    "return all tax identifiers for sautr" in new TestCase {
      val expectedPath = s"/entity-resolver/sa/${sautr.value}"
      val responseJson = responseJsonFor(sautr,nino)
      val response = wsResponseFor(responseJson, Status.OK)

      when(wsClient.url(expectedPath)).thenReturn(mockRequest)
      when(mockRequest.get()).thenReturn(Future.successful(response))

      val result = entityResolverConnector.getTaxIdentifiers(sautr).futureValue

      result.size shouldBe(2)
      result should contain (nino)
      result should contain (sautr)
    }

    "return all tax identifiers for nino" in new TestCase {
      val expectedPath = s"/entity-resolver/paye/${nino.value}"
      val responseJson = responseJsonFor(sautr,nino)
      val response = wsResponseFor(responseJson, Status.OK)

      when(wsClient.url(expectedPath)).thenReturn(mockRequest)
      when(mockRequest.get()).thenReturn(Future.successful(response))

      val result = entityResolverConnector.getTaxIdentifiers(nino).futureValue

      result.size shouldBe(2)
      result should contain (nino)
      result should contain (sautr)
    }

    "return empty sequence" in new TestCase {

    }


  }

  trait TestCase extends MockitoSugar {
    val sautr = TaxIdentifier("sautr", Random.nextInt(1000000).toString)
    val nino = TaxIdentifier("nino", "NA000914D")

    implicit val hc: HeaderCarrier = HeaderCarrier()

    lazy val serviceConfig = mock[ServicesConfig]
    lazy val wsClient = mock[WSClient]
    lazy val mockRequest = mock[WSRequest]


    lazy val entityResolverConnector = new EntityResolverConnector(wsClient, serviceConfig){
      override def serviceUrl: String = ""
    }



    def wsResponseFor(body: String, status: Int): WSResponse = {
      val myMock = mock[WSResponse]
      when(myMock.body).thenReturn(body)
      when(myMock.status).thenReturn(status)
      myMock
    }


    def responseJsonFor(taxIds: TaxIdentifier*) = {
      val taxIdsJson: Seq[(String, JsValue)] = taxIds.map {
        taxId => taxId.name -> JsString(taxId.value)
      }
      taxIdsJson.foldLeft(Json.obj("_id" -> "6a048719-3d4b-4a3e-9440-17b238807bc9"))( _ + _ ).map(_.toString())
    }
  }
}
