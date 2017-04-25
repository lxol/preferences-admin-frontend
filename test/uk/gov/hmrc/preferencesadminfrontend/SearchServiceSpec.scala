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

import org.mockito.{ArgumentMatcher, Mockito}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.mockito.MockitoSugar
import play.api.Configuration
import play.api.libs.json.Json
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent
import uk.gov.hmrc.play.config.inject.AppName
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.preferencesadminfrontend.connectors._
import uk.gov.hmrc.preferencesadminfrontend.controllers.model.User
import uk.gov.hmrc.preferencesadminfrontend.services._
import uk.gov.hmrc.preferencesadminfrontend.services.model.{Email, Preference, TaxIdentifier}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SearchServiceSpec extends UnitSpec with MockitoSugar with ScalaFutures with IntegrationPatience {

  implicit val hc = HeaderCarrier()

  "getPreferences" should {

    "return preference for nino user when it exists" in new TestCase {
      when(entityResolverConnector.getPreferenceDetails(validNino)).thenReturn(Future.successful(optedInPreferenceDetails))
      when(entityResolverConnector.getTaxIdentifiers(validNino)).thenReturn(Future.successful(taxIdentifiers))

      val result = searchService.getPreference(validNino).futureValue

      result shouldBe Some(optedInPreference)

      val expectedAuditEvent = searchService.createSearchEvent("user", validNino, Some(optedInPreference))
      Mockito.verify(auditConnector).sendEvent(argThat(isSimilar(expectedAuditEvent)))(any(), any())
    }

    "return preference for utr user when it exists" in new TestCase {
      when(entityResolverConnector.getPreferenceDetails(validSaUtr)).thenReturn(Future.successful(optedInPreferenceDetails))
      when(entityResolverConnector.getTaxIdentifiers(validSaUtr)).thenReturn(Future.successful(taxIdentifiers))

      val result = searchService.getPreference(validSaUtr).futureValue


      result shouldBe Some(optedInPreference)

      val expectedAuditEvent = searchService.createSearchEvent("user", validSaUtr, Some(optedInPreference))
      Mockito.verify(auditConnector).sendEvent(argThat(isSimilar(expectedAuditEvent)))(any(), any())
    }

    "return preference for utr user who has opted out" in new TestCase {
      when(entityResolverConnector.getPreferenceDetails(validSaUtr)).thenReturn(Future.successful(optedOutPreferenceDetails))
      when(entityResolverConnector.getTaxIdentifiers(validSaUtr)).thenReturn(Future.successful(taxIdentifiers))

      val result = searchService.getPreference(validSaUtr).futureValue

      result shouldBe Some(optedOutPreference)

      val expectedAuditEvent = searchService.createSearchEvent("user", validSaUtr, Some(optedOutPreference))
      Mockito.verify(auditConnector).sendEvent(argThat(isSimilar(expectedAuditEvent)))(any(), any())
    }

    "return None if the saUtr identifier does not exist" in new TestCase {
      val preferenceDetails = None
      when(entityResolverConnector.getPreferenceDetails(validSaUtr)).thenReturn(Future.successful(preferenceDetails))
      override val taxIdentifiers = Seq()
      when(entityResolverConnector.getTaxIdentifiers(validSaUtr)).thenReturn(Future.successful(taxIdentifiers))

      val result = searchService.getPreference(validSaUtr).futureValue

      result shouldBe None

      val expectedAuditEvent = searchService.createSearchEvent("user", validSaUtr, None)
      Mockito.verify(auditConnector).sendEvent(argThat(isSimilar(expectedAuditEvent)))(any(), any())
    }

  }

  "optOut" should {


    "call entity resolver to opt the user out" in new TestCase {
      when(entityResolverConnector.getPreferenceDetails(validSaUtr)).thenReturn(Future.successful(optedInPreferenceDetails), Future.successful(optedOutPreferenceDetails))
      when(entityResolverConnector.getTaxIdentifiers(validSaUtr)).thenReturn(Future.successful(taxIdentifiers))
      when(entityResolverConnector.optOut(validSaUtr)).thenReturn(Future.successful(OptedOut))

      val result = searchService.optOut(validSaUtr).futureValue
      verify(entityResolverConnector, times(1)).optOut(validSaUtr)
    }

    "create an audit event when the user is opted out" in new TestCase {
      when(entityResolverConnector.getPreferenceDetails(validSaUtr)).thenReturn(Future.successful(optedInPreferenceDetails), Future.successful(optedOutPreferenceDetails))
      when(entityResolverConnector.getTaxIdentifiers(validSaUtr)).thenReturn(Future.successful(taxIdentifiers))
      when(entityResolverConnector.optOut(validSaUtr)).thenReturn(Future.successful(OptedOut))
      when(auditConnector.sendEvent(any())(any(),any())).thenReturn(Future.successful(AuditResult.Success))

      val result = searchService.optOut(validSaUtr).futureValue
      verify(auditConnector, times(1)).sendEvent(any())(any(),any())

      val expectedAuditEvent = searchService.createOptOutEvent("me", validSaUtr, Some(optedInPreference), Some(optedOutPreference), OptedOut)
      Mockito.verify(auditConnector).sendEvent(argThat(isSimilar(expectedAuditEvent)))(any(), any())
    }

    "create an audit event when the user is not opted out as it is not found" in new TestCase {
      when(entityResolverConnector.getPreferenceDetails(validSaUtr)).thenReturn(Future.successful(None), Future.successful(None))
      when(entityResolverConnector.getTaxIdentifiers(validSaUtr)).thenReturn(Future.successful(Seq.empty))
      when(entityResolverConnector.optOut(validSaUtr)).thenReturn(Future.successful(PreferenceNotFound))
      when(auditConnector.sendEvent(any())(any(),any())).thenReturn(Future.successful(AuditResult.Success))

      val result = searchService.optOut(validSaUtr).futureValue
      verify(auditConnector, times(1)).sendEvent(any())(any(),any())

      val expectedAuditEvent = searchService.createOptOutEvent("me", validSaUtr, None, None, PreferenceNotFound)
      Mockito.verify(auditConnector).sendEvent(argThat(isSimilar(expectedAuditEvent)))(any(), any())
    }

    "create an audit event when the user is already opted out" in new TestCase {
      when(entityResolverConnector.getPreferenceDetails(validSaUtr)).thenReturn(Future.successful(optedOutPreferenceDetails), Future.successful(optedOutPreferenceDetails))
      when(entityResolverConnector.getTaxIdentifiers(validSaUtr)).thenReturn(Future.successful(taxIdentifiers))
      when(entityResolverConnector.optOut(validSaUtr)).thenReturn(Future.successful(AlreadyOptedOut))
      when(auditConnector.sendEvent(any())(any(),any())).thenReturn(Future.successful(AuditResult.Success))

      val result = searchService.optOut(validSaUtr).futureValue
      verify(auditConnector, times(1)).sendEvent(any())(any(),any())

      val expectedAuditEvent = searchService.createOptOutEvent("me", validSaUtr, Some(optedOutPreference), Some(optedOutPreference), AlreadyOptedOut)
      Mockito.verify(auditConnector).sendEvent(argThat(isSimilar(expectedAuditEvent)))(any(), any())
    }
  }

  "createSearchEvent" should {
    "generate the correct event when the preference exists" in new TestCase {
      val preference = Preference(paperless = true, email = Some(Email(address = "john.doe@digital.hmrc.gov.uk", verified = true)), taxIdentifiers = Seq(TaxIdentifier("sautr", "123"),TaxIdentifier("nino", "ABC")))
      val event = searchService.createSearchEvent("me", TaxIdentifier("sautr", "123"), Some(preference))

      event.auditSource shouldBe "preferences-admin-frontend"
      event.auditType shouldBe "TxSucceeded"
      event.detail shouldBe Json.obj(
        "user" -> "me",
        "query" -> Json.obj("name" -> "sautr", "value" -> "123"),
        "result" -> "Found",
        "preference" -> Json.obj(
          "paperless" -> true,
          "email" -> Json.obj("address" -> "john.doe@digital.hmrc.gov.uk", "verified" -> true),
          "taxIdentifiers" -> Json.arr(Json.obj("name" -> "sautr", "value" -> "123"), Json.obj("name" -> "nino", "value" -> "ABC"))
        )
      )
      event.tags("transactionName") shouldBe "Paperless opt out search"
    }

    "generate the correct event when the preference does not exist" in new TestCase {
      val event = searchService.createSearchEvent("me", TaxIdentifier("sautr", "123"), None)

      event.auditSource shouldBe "preferences-admin-frontend"
      event.auditType shouldBe "TxSucceeded"
      event.detail shouldBe Json.obj(
        "user" -> "me",
        "query" -> Json.obj("name" -> "sautr", "value" -> "123"),
        "result" -> "Not found"
      )
      event.tags("transactionName") shouldBe "Paperless opt out search"

    }
  }

  "createOptoutEvent" should {

    "generate the correct event user is opted out" in new TestCase {
      val event = searchService.createOptOutEvent("me", validSaUtr, Some(optedInPreference), Some(optedOutPreference), OptedOut)

      event.auditSource shouldBe "preferences-admin-frontend"
      event.auditType shouldBe "TxSucceeded"
      event.detail shouldBe Json.obj(
        "user" -> "me",
        "query" -> Json.obj("name" -> validSaUtr.name, "value" -> validSaUtr.value),
        "originalPreference" -> Json.obj(
          "paperless" -> true,
          "email" -> Json.obj("address" -> "john.doe@digital.hmrc.gov.uk", "verified" -> true),
          "taxIdentifiers" -> Json.arr(Json.obj("name" -> validSaUtr.name, "value" -> validSaUtr.value), Json.obj("name" -> validNino.name, "value" -> validNino.value))
        ),
        "newPreference" -> Json.obj(
          "paperless" -> false,
          "taxIdentifiers" -> Json.arr(Json.obj("name" -> validSaUtr.name, "value" -> validSaUtr.value), Json.obj("name" -> validNino.name, "value" -> validNino.value))
        )
      )
      event.tags("transactionName") shouldBe "Manual opt out from paperless"
    }

    "generate the correct event when the preference already opted out" in new TestCase {
      val event = searchService.createOptOutEvent("me", validSaUtr, Some(optedOutPreference), Some(optedOutPreference), AlreadyOptedOut)

      event.auditSource shouldBe "preferences-admin-frontend"
      event.auditType shouldBe "TxFailed"
      event.detail shouldBe Json.obj(
        "user" -> "me",
        "query" -> Json.obj("name" -> validSaUtr.name, "value" -> validSaUtr.value),
        "originalPreference" -> Json.obj(
          "paperless" -> false,
          "taxIdentifiers" -> Json.arr(Json.obj("name" -> validSaUtr.name, "value" -> validSaUtr.value), Json.obj("name" -> validNino.name, "value" -> validNino.value))
        ),
        "newPreference" -> Json.obj(
          "paperless" -> false,
          "taxIdentifiers" -> Json.arr(Json.obj("name" -> validSaUtr.name, "value" -> validSaUtr.value), Json.obj("name" -> validNino.name, "value" -> validNino.value))
        ),
        "reasonOfFailure" -> "Preference already opted out"
      )
      event.tags("transactionName") shouldBe "Manual opt out from paperless"

    }

    "generate the correct event when the preference does not exist" in new TestCase {
      val event = searchService.createOptOutEvent("me", validSaUtr, None, None, PreferenceNotFound)

      event.auditSource shouldBe "preferences-admin-frontend"
      event.auditType shouldBe "TxFailed"
      event.detail shouldBe Json.obj(
        "user" -> "me",
        "query" -> Json.obj("name" -> validSaUtr.name, "value" -> validSaUtr.value),
        "reasonOfFailure" -> "Preference not found"
      )
      event.tags("transactionName") shouldBe "Manual opt out from paperless"

    }

    "generate the correct event when entity is not found" in new TestCase {
      val event = searchService.createOptOutEvent("me", validSaUtr, None, None, PreferenceNotFound)

      event.auditSource shouldBe "preferences-admin-frontend"
      event.auditType shouldBe "TxFailed"
      event.detail shouldBe Json.obj(
        "user" -> "me",
        "query" -> Json.obj("name" -> validSaUtr.name, "value" -> validSaUtr.value),
        "reasonOfFailure" -> "Preference not found"
      )
      event.tags("transactionName") shouldBe "Manual opt out from paperless"

    }

  }

  trait TestCase {

    implicit val user = User("me", "mySecretPassword")

    val validSaUtr = TaxIdentifier("sautr", "123456789")
    val validNino = TaxIdentifier("nino", "CE067583D")
    val invalidNino = TaxIdentifier("nino", "123123456S")

    val verifiedEmail = Email("john.doe@digital.hmrc.gov.uk", verified = true)
    val optedInPreferenceDetails = Some(PreferenceDetails(paperless = true, Some(verifiedEmail)))
    val optedOutPreferenceDetails = Some(PreferenceDetails(paperless = false, None))
    val taxIdentifiers = Seq(validSaUtr, validNino)

    val optedInPreference = Preference(paperless = true, email = Some(verifiedEmail), taxIdentifiers = taxIdentifiers)
    val optedOutPreference = Preference(paperless = false, email = None, taxIdentifiers = taxIdentifiers)

    val auditConnector = mock[AuditConnector]
    val entityResolverConnector = mock[EntityResolverConnector]
    val appName = new AppName {
      protected def appNameConfiguration: Configuration = ???
      override def appName: String = "preferences-admin-frontend"
    }
    val searchService = new SearchService(entityResolverConnector, auditConnector, appName)

    def isSimilar(expected: ExtendedDataEvent): ArgumentMatcher[ExtendedDataEvent] = {
      new ArgumentMatcher[ExtendedDataEvent]() {
        def matches(t: ExtendedDataEvent): Boolean = {
          t.auditSource == expected.auditSource &&
            t.auditType == expected.auditType &&
            t.detail == expected.detail &&
            t.tags == expected.tags
        }
      }
    }
  }
}