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
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.preferencesadminfrontend.connectors.{EntityResolverConnector, PreferenceDetails}
import uk.gov.hmrc.preferencesadminfrontend.services._
import uk.gov.hmrc.preferencesadminfrontend.services.model.{Email, TaxIdentifier}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SearchServiceSpec extends UnitSpec with MockitoSugar with ScalaFutures {


  "getPreferences" should {

    val validSaUtr = TaxIdentifier("sautr", "123456789")
    val validNino = TaxIdentifier("nino", "SS123456S")

    val entityResolverConnector = mock[EntityResolverConnector]
    val searchService = new SearchService(entityResolverConnector)

    implicit val hc = HeaderCarrier()

    "return preferences for nino user when it exists" in new TestCase {

      val preferenceDetails = Some(PreferenceDetails(paperless = true, Email("john.doe@digital.hmrc.gov.uk", verified = true)))
      when(entityResolverConnector.getPreferenceDetails(validNino)).thenReturn(Future.successful(preferenceDetails))
      val taxIdentifiers = Seq(validNino, validSaUtr)
      when(entityResolverConnector.getTaxIdentifiers(validNino)).thenReturn(Future.successful(taxIdentifiers))

      val result = searchService.getPreference(validNino).futureValue

      result match {
        case PreferenceFound(preference) => {
          preference.paperless shouldBe true
          preference.email shouldBe Email("john.doe@digital.hmrc.gov.uk", true)
          preference.taxIdentifiers shouldBe Seq(validNino, validSaUtr)
        }
        case _ => fail()
      }
    }

    "return preferences for utr user when it exists" in new TestCase {
      val preferenceDetails = Some(PreferenceDetails(paperless = true, Email("john.doe@digital.hmrc.gov.uk", verified = true)))
      when(entityResolverConnector.getPreferenceDetails(validSaUtr)).thenReturn(Future.successful(preferenceDetails))
      val taxIdentifiers = Seq(validNino, validSaUtr)
      when(entityResolverConnector.getTaxIdentifiers(validSaUtr)).thenReturn(Future.successful(taxIdentifiers))

      val result = searchService.getPreference(validSaUtr).futureValue

      result match {
        case PreferenceFound(preference) => {
          preference.paperless shouldBe true
          preference.email shouldBe Email("john.doe@digital.hmrc.gov.uk", true)
          preference.taxIdentifiers shouldBe Seq(validNino, validSaUtr)
        }
        case _ => fail()
      }
    }

    "return PreferenceNotFound if the saUtr identifier does not exist" in new TestCase {
      val preferenceDetails = None
      when(entityResolverConnector.getPreferenceDetails(validSaUtr)).thenReturn(Future.successful(preferenceDetails))
      val taxIdentifiers = Seq()
      when(entityResolverConnector.getTaxIdentifiers(validSaUtr)).thenReturn(Future.successful(taxIdentifiers))

      val result = searchService.getPreference(validSaUtr).futureValue

      result shouldBe PreferenceNotFound
    }

    "return InvalidTaxIdentifier if the nino is invalid" in new TestCase {
      val result = searchService.getPreference(invalidNino).futureValue

      verifyZeroInteractions(entityResolverConnector)
      result shouldBe InvalidTaxIdentifier
    }

    "return ErrorMessage if something goes wrong when calling downstream dependencies" in new TestCase {
      val taxIdentifiers = Seq()
      when(entityResolverConnector.getPreferenceDetails(validSaUtr)).thenReturn(Future.failed(new Throwable("my-message")))
      when(entityResolverConnector.getTaxIdentifiers(validSaUtr)).thenReturn(Future.successful(taxIdentifiers))

      val result = searchService.getPreference(validSaUtr).futureValue

      result should matchPattern { case Failure(_) => }
    }

    "return ErrorMessage if something goes wrong when calling downstream dependencies v2" in new TestCase {
      when(entityResolverConnector.getPreferenceDetails(validSaUtr)).thenReturn(Future.successful(None))
      when(entityResolverConnector.getTaxIdentifiers(validSaUtr)).thenThrow(new RuntimeException("my-message"))

      val result = searchService.getPreference(validSaUtr).futureValue

      result should matchPattern { case Failure(_) => }
    }
  }

  "isValid" should {

    "return true for non nino tax identifiers" in new TestCase {
      searchService.isValid(validSaUtr) shouldBe true
    }

    "return true for a valid nino" in new TestCase {
      searchService.isValid(validNino) shouldBe true
    }

    "return false for an invalid nino" in new TestCase {
      searchService.isValid(invalidNino) shouldBe false
    }
  }

  trait TestCase {
    val validSaUtr = TaxIdentifier("sautr", "123456789")
    val validNino = TaxIdentifier("nino", "CE067583D")
    val invalidNino = TaxIdentifier("nino", "123123456S")

    val entityResolverConnector = mock[EntityResolverConnector]
    val searchService = new SearchService(entityResolverConnector)
  }
}