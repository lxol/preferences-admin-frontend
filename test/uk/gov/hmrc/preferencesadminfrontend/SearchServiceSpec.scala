/*
 * Copyright 2018 HM Revenue & Customs
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

import org.joda.time.{DateTime, DateTimeZone}
import org.mockito.ArgumentMatcher
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.mockito.MockitoSugar
import play.api.Configuration
import play.api.libs.json.Json
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent
import uk.gov.hmrc.play.config.inject.AppName
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.preferencesadminfrontend.connectors._
import uk.gov.hmrc.preferencesadminfrontend.controllers.model.User
import uk.gov.hmrc.preferencesadminfrontend.services._
import uk.gov.hmrc.preferencesadminfrontend.services.model.{Email, Preference, TaxIdentifier}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

class SearchServiceSpec extends UnitSpec with MockitoSugar with ScalaFutures with IntegrationPatience {

  implicit val hc = HeaderCarrier()

  "getPreferences" should {

    "return preference for nino user when it exists" in new TestCase {
      when(entityResolverConnector.getPreferenceDetails(validNino)).thenReturn(Future.successful(optedInPreferenceDetails))
      when(entityResolverConnector.getTaxIdentifiers(validNino)).thenReturn(Future.successful(taxIdentifiers))

      searchService.searchPreference(validNino).futureValue shouldBe Some(optedInPreference)

      val expectedAuditEvent = searchService.createSearchEvent("me", validNino, Some(optedInPreference))
      verify(auditConnector).sendExtendedEvent(argThat(isSimilar(expectedAuditEvent)))(any(), any())
    }

    "return preference for utr user when it exists" in new TestCase {
      when(entityResolverConnector.getPreferenceDetails(validSaUtr)).thenReturn(Future.successful(optedInPreferenceDetails))
      when(entityResolverConnector.getTaxIdentifiers(validSaUtr)).thenReturn(Future.successful(taxIdentifiers))

      searchService.searchPreference(validSaUtr).futureValue shouldBe Some(optedInPreference)

      val expectedAuditEvent = searchService.createSearchEvent("me", validSaUtr, Some(optedInPreference))
      verify(auditConnector).sendExtendedEvent(argThat(isSimilar(expectedAuditEvent)))(any(), any())
    }

    "return preference for utr user who has opted out" in new TestCase {
      when(entityResolverConnector.getPreferenceDetails(validSaUtr)).thenReturn(Future.successful(optedOutPreferenceDetails))
      when(entityResolverConnector.getTaxIdentifiers(validSaUtr)).thenReturn(Future.successful(taxIdentifiers))

      searchService.searchPreference(validSaUtr).futureValue shouldBe Some(optedOutPreference)

      val expectedAuditEvent = searchService.createSearchEvent("me", validSaUtr, Some(optedOutPreference))
      verify(auditConnector).sendExtendedEvent(argThat(isSimilar(expectedAuditEvent)))(any(), any())
    }

    "return None if the saUtr identifier does not exist" in new TestCase {
      val preferenceDetails = None
      when(entityResolverConnector.getPreferenceDetails(validSaUtr)).thenReturn(Future.successful(preferenceDetails))
      override val taxIdentifiers = Seq()
      when(entityResolverConnector.getTaxIdentifiers(validSaUtr)).thenReturn(Future.successful(taxIdentifiers))

      searchService.searchPreference(validSaUtr).futureValue shouldBe None

      val expectedAuditEvent = searchService.createSearchEvent("me", validSaUtr, None)
      verify(auditConnector).sendExtendedEvent(argThat(isSimilar(expectedAuditEvent)))(any(), any())
    }

  }

  "optOut" should {


    "call entity resolver to opt the user out" in new TestCase {
      when(entityResolverConnector.getPreferenceDetails(validSaUtr)).thenReturn(Future.successful(optedInPreferenceDetails), Future.successful(optedOutPreferenceDetails))
      when(entityResolverConnector.getTaxIdentifiers(validSaUtr)).thenReturn(Future.successful(taxIdentifiers))
      when(entityResolverConnector.optOut(validSaUtr)).thenReturn(Future.successful(OptedOut))

      searchService.optOut(validSaUtr, "my optOut reason").futureValue shouldBe OptedOut
      verify(entityResolverConnector, times(1)).optOut(validSaUtr)
    }

    "create an audit event when the user is opted out" in new TestCase {
      when(entityResolverConnector.getPreferenceDetails(validSaUtr)).thenReturn(Future.successful(optedInPreferenceDetails), Future.successful(optedOutPreferenceDetails))
      when(entityResolverConnector.getTaxIdentifiers(validSaUtr)).thenReturn(Future.successful(taxIdentifiers))
      when(entityResolverConnector.optOut(validSaUtr)).thenReturn(Future.successful(OptedOut))
      when(auditConnector.sendEvent(any())(any(),any())).thenReturn(Future.successful(AuditResult.Success))

      searchService.optOut(validSaUtr,"my optOut reason").futureValue shouldBe OptedOut

      val expectedAuditEvent = searchService.createOptOutEvent("me", validSaUtr, Some(optedInPreference), Some(optedOutPreference), OptedOut, "my optOut reason")
      verify(auditConnector).sendExtendedEvent(argThat(isSimilar(expectedAuditEvent)))(any(), any())
    }

    "create an audit event when the user is not opted out as it is not found" in new TestCase {
      when(entityResolverConnector.getPreferenceDetails(validSaUtr)).thenReturn(Future.successful(None), Future.successful(None))
      when(entityResolverConnector.getTaxIdentifiers(validSaUtr)).thenReturn(Future.successful(Seq.empty))
      when(entityResolverConnector.optOut(validSaUtr)).thenReturn(Future.successful(PreferenceNotFound))
      when(auditConnector.sendEvent(any())(any(),any())).thenReturn(Future.successful(AuditResult.Success))

      searchService.optOut(validSaUtr, "my optOut reason").futureValue shouldBe PreferenceNotFound

      val expectedAuditEvent = searchService.createOptOutEvent("me", validSaUtr, None, None, PreferenceNotFound, "my optOut reason")
      verify(auditConnector).sendExtendedEvent(argThat(isSimilar(expectedAuditEvent)))(any(), any())
    }

    "create an audit event when the user is already opted out" in new TestCase {
      when(entityResolverConnector.getPreferenceDetails(validSaUtr)).thenReturn(Future.successful(optedOutPreferenceDetails), Future.successful(optedOutPreferenceDetails))
      when(entityResolverConnector.getTaxIdentifiers(validSaUtr)).thenReturn(Future.successful(taxIdentifiers))
      when(entityResolverConnector.optOut(validSaUtr)).thenReturn(Future.successful(AlreadyOptedOut))
      when(auditConnector.sendEvent(any())(any(),any())).thenReturn(Future.successful(AuditResult.Success))

      searchService.optOut(validSaUtr, "my optOut reason").futureValue shouldBe AlreadyOptedOut

      val expectedAuditEvent = searchService.createOptOutEvent("me", validSaUtr, Some(optedOutPreference), Some(optedOutPreference), AlreadyOptedOut, "my optOut reason")
      verify(auditConnector).sendExtendedEvent(argThat(isSimilar(expectedAuditEvent)))(any(), any())
    }
  }

  "createSearchEvent" should {
    "generate the correct event when the preference exists" in new TestCase {
      val preference = Preference(genericPaperless = true, genericUpdatedAt = genericUpdatedAt, taxCreditsPaperless = true, taxCreditsUpdatedAt = taxCreditsUpdatedAt,
        email = Some(Email(address = "john.doe@digital.hmrc.gov.uk", verified = true, verifiedOn = verifiedOn)), taxIdentifiers = Seq(TaxIdentifier("sautr", "123"),TaxIdentifier("nino", "ABC")))
      val event = searchService.createSearchEvent("me", TaxIdentifier("sautr", "123"), Some(preference))

      event.auditSource shouldBe "preferences-admin-frontend"
      event.auditType shouldBe "TxSucceeded"
      event.detail shouldBe Json.obj(
        "user" -> "me",
        "query" -> Json.obj("name" -> "sautr", "value" -> "123"),
        "result" -> "Found",
        "preference" -> Json.obj(
          "genericPaperless" -> true,
          "genericUpdatedAt" -> 1518652800000L,
          "taxCreditsPaperless" -> true,
          "taxCreditsUpdatedAt" -> 1518652800000L,
          "email" -> Json.obj("address" -> "john.doe@digital.hmrc.gov.uk", "verified" -> true, "verifiedOn" -> 1518652800000L),
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
      val event = searchService.createOptOutEvent("me", validSaUtr, Some(optedInPreference), Some(optedOutPreference), OptedOut, "my optOut reason")

      event.auditSource shouldBe "preferences-admin-frontend"
      event.auditType shouldBe "TxSucceeded"
      event.detail shouldBe Json.obj(
        "user" -> "me",
        "query" -> Json.obj("name" -> validSaUtr.name, "value" -> validSaUtr.value),
        "optOutReason" -> "my optOut reason",
        "originalPreference" -> Json.obj(
          "genericPaperless" -> true,
          "genericUpdatedAt" -> 1518652800000L,
          "taxCreditsPaperless" -> false,
          "taxCreditsUpdatedAt" -> 1518652800000L,
          "email" -> Json.obj("address" -> "john.doe@digital.hmrc.gov.uk", "verified" -> true, "verifiedOn" -> 1518652800000L),
          "taxIdentifiers" -> Json.arr(Json.obj("name" -> validSaUtr.name, "value" -> validSaUtr.value), Json.obj("name" -> validNino.name, "value" -> validNino.value))
        ),
        "newPreference" -> Json.obj(
          "genericPaperless" -> false,
          "genericUpdatedAt" -> 1518652800000L,
          "taxCreditsPaperless" -> false,
          "taxCreditsUpdatedAt" -> 1518652800000L,
          "taxIdentifiers" -> Json.arr(Json.obj("name" -> validSaUtr.name, "value" -> validSaUtr.value), Json.obj("name" -> validNino.name, "value" -> validNino.value))
        )
      )
      event.tags("transactionName") shouldBe "Manual opt out from paperless"
    }

    "generate the correct event when the preference already opted out" in new TestCase {
      val event = searchService.createOptOutEvent("me", validSaUtr, Some(optedOutPreference), Some(optedOutPreference), AlreadyOptedOut, "my optOut reason")

      event.auditSource shouldBe "preferences-admin-frontend"
      event.auditType shouldBe "TxFailed"
      event.detail shouldBe Json.obj(
        "user" -> "me",
        "query" -> Json.obj("name" -> validSaUtr.name, "value" -> validSaUtr.value),
        "optOutReason" -> "my optOut reason",
        "originalPreference" -> Json.obj(
          "genericPaperless" -> false,
          "genericUpdatedAt" -> 1518652800000L,
          "taxCreditsPaperless" -> false,
          "taxCreditsUpdatedAt" -> 1518652800000L,
          "taxIdentifiers" -> Json.arr(Json.obj("name" -> validSaUtr.name, "value" -> validSaUtr.value), Json.obj("name" -> validNino.name, "value" -> validNino.value))
        ),
        "newPreference" -> Json.obj(
          "genericPaperless" -> false,
          "genericUpdatedAt" -> 1518652800000L,
          "taxCreditsPaperless" -> false,
          "taxCreditsUpdatedAt" -> 1518652800000L,
          "taxIdentifiers" -> Json.arr(Json.obj("name" -> validSaUtr.name, "value" -> validSaUtr.value), Json.obj("name" -> validNino.name, "value" -> validNino.value))
        ),
        "reasonOfFailure" -> "Preference already opted out"
      )
      event.tags("transactionName") shouldBe "Manual opt out from paperless"

    }

    "generate the correct event when the preference does not exist" in new TestCase {
      val event = searchService.createOptOutEvent("me", validSaUtr, None, None, PreferenceNotFound, "my optOut reason")

      event.auditSource shouldBe "preferences-admin-frontend"
      event.auditType shouldBe "TxFailed"
      event.detail shouldBe Json.obj(
        "user" -> "me",
        "query" -> Json.obj("name" -> validSaUtr.name, "value" -> validSaUtr.value),
        "optOutReason" -> "my optOut reason",
        "reasonOfFailure" -> "Preference not found"
      )
      event.tags("transactionName") shouldBe "Manual opt out from paperless"

    }

    "generate the correct event when entity is not found" in new TestCase {
      val event = searchService.createOptOutEvent("me", validSaUtr, None, None, PreferenceNotFound, "my optOut reason")

      event.auditSource shouldBe "preferences-admin-frontend"
      event.auditType shouldBe "TxFailed"
      event.detail shouldBe Json.obj(
        "user" -> "me",
        "query" -> Json.obj("name" -> validSaUtr.name, "value" -> validSaUtr.value),
        "optOutReason" -> "my optOut reason",
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

    val genericUpdatedAt = Some(new DateTime(2018, 2, 15, 0, 0, DateTimeZone.UTC))
    val taxCreditsUpdatedAt = Some(new DateTime(2018, 2, 15, 0, 0, DateTimeZone.UTC))
    val verifiedOn = Some(new DateTime(2018, 2, 15, 0, 0, DateTimeZone.UTC))

    val verifiedEmail = Email("john.doe@digital.hmrc.gov.uk", verified = true, verifiedOn = verifiedOn)

    def preferenceDetails(genericPaperless: Boolean, taxCreditsPaperless: Boolean) = {
      val email = if (genericPaperless | taxCreditsPaperless) Some(verifiedEmail) else None
      Some(PreferenceDetails(genericPaperless, genericUpdatedAt, taxCreditsPaperless, taxCreditsUpdatedAt, email))
    }

    val optedInPreferenceDetails = preferenceDetails(genericPaperless = true, taxCreditsPaperless = false)
    val optedOutPreferenceDetails = preferenceDetails(genericPaperless = false, taxCreditsPaperless = false)

    val taxIdentifiers = Seq(validSaUtr, validNino)

    val optedInPreference = Preference(genericPaperless = true, genericUpdatedAt = genericUpdatedAt, taxCreditsPaperless = false, taxCreditsUpdatedAt = taxCreditsUpdatedAt,
      email = Some(verifiedEmail), taxIdentifiers = taxIdentifiers)
    val optedOutPreference = Preference(genericPaperless = false, genericUpdatedAt = genericUpdatedAt, taxCreditsPaperless = false, taxCreditsUpdatedAt = taxCreditsUpdatedAt,
      email = None, taxIdentifiers = taxIdentifiers)

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