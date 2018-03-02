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

import akka.stream.Materializer
import org.joda.time.{DateTime, DateTimeZone}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.mockito.{ArgumentMatcher, ArgumentMatchers, Mockito}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Configuration
import play.api.http.Status
import play.api.i18n.MessagesApi
import play.api.test.Helpers.{headers, _}
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.preferencesadminfrontend.config.FrontendAppConfig
import uk.gov.hmrc.preferencesadminfrontend.connectors.OptedOut
import uk.gov.hmrc.preferencesadminfrontend.controllers.SearchController
import uk.gov.hmrc.preferencesadminfrontend.controllers.model.User
import uk.gov.hmrc.preferencesadminfrontend.services._
import uk.gov.hmrc.preferencesadminfrontend.services.model.{Email, Preference, TaxIdentifier}
import uk.gov.hmrc.preferencesadminfrontend.utils.CSRFTest

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

class SearchControllerSpec extends UnitSpec with CSRFTest with ScalaFutures with GuiceOneAppPerSuite {
  implicit val hc = HeaderCarrier()
  implicit val messagesApi = app.injector.instanceOf[MessagesApi]
  implicit val materializer = app.injector.instanceOf[Materializer]
  val playConfiguration = app.injector.instanceOf[Configuration]

  "showSearchPage" should {

    "return ok if session is authorised" in new TestCase {
      val result = searchController.showSearchPage("","")(addToken(FakeRequest().withSession(User.sessionKey -> "user")))

      status(result) shouldBe Status.OK
    }

    "redirect to login page if not authorised" in new TestCase {
      val result = searchController.showSearchPage("","")(addToken(FakeRequest().withSession()))

      status(result) shouldBe Status.SEE_OTHER
      headers(result) should contain("Location" -> "/paperless/admin")
    }
  }

  "search(taxIdentifier)" should {

    val queryParamsForValidNino = "?name=nino&value=CE067583D"
    val queryParamsForEmailid = "?name=email&value=test@test.com"
    val queryParamsForValidLowercaseNino = "?name=nino&value=ce067583d"
    val queryParamsForInvalidNino = "?name=nino&value=1234567"
    val genericUpdatedAt = Some(new DateTime(2018, 2, 15, 0, 0, DateTimeZone.UTC))
    val taxCreditsUpdatedAt = Some(new DateTime(2018, 2, 15, 0, 0, DateTimeZone.UTC))
    val verifiedOn = Some(new DateTime(2018, 2, 15, 0, 0, DateTimeZone.UTC))

    "return a preference if tax identifier exists" in new TestCase {

      val preference = Preference(genericPaperless = true, genericUpdatedAt = genericUpdatedAt, taxCreditsPaperless = true,  taxCreditsUpdatedAt = taxCreditsUpdatedAt,
        Some(Email("john.doe@digital.hmrc.gov.uk", verified = true, verifiedOn = verifiedOn)), Seq())
      when(searchServiceMock.searchPreference(any())(any(), any(), any())).thenReturn(Future.successful(List(preference)))

      val result = searchController.search(addToken(FakeRequest("GET", queryParamsForValidNino).withSession(User.sessionKey -> "user")))

      status(result) shouldBe Status.OK
      val body: String = bodyOf(result).futureValue
      body should include ("john.doe@digital.hmrc.gov.uk")
      body should include ("15 February 2018 AM 12:0:0s")
    }

    "return a preference if email address exists" in new TestCase {
      val preference = Preference(genericPaperless = true, genericUpdatedAt = genericUpdatedAt, taxCreditsPaperless = true,  taxCreditsUpdatedAt = taxCreditsUpdatedAt,
        Some(Email("test@test.com", verified = true, verifiedOn = verifiedOn)), Seq())
      when(searchServiceMock.searchPreference(any())(any(), any(), any())).thenReturn(Future.successful(List(preference)))

      val result = searchController.search(addToken(FakeRequest("GET", queryParamsForEmailid).withSession(User.sessionKey -> "user")))

      status(result) shouldBe Status.OK
      val body: String = bodyOf(result).futureValue
      body should include ("test@test.com")
      body should include ("15 February 2018 AM 12:0:0s")
    }

    "return a not found error message if the preference associated with that emailid is not found" in new TestCase {
      when(searchServiceMock.searchPreference(any())(any(), any(), any())).thenReturn(Future.successful(Nil))
      Mockito.reset(auditConnectorMock)
      val result = searchController.search(addToken(FakeRequest("GET", queryParamsForEmailid).withSession(User.sessionKey -> "user")))

      status(result) shouldBe Status.OK
      bodyOf(result).futureValue should include ("No paperless preference found for that identifier.")
    }

    "include a hidden form to opt the user out" in new TestCase {

      val preference = Preference(genericPaperless = true, genericUpdatedAt = genericUpdatedAt, taxCreditsPaperless = true, taxCreditsUpdatedAt = taxCreditsUpdatedAt,
        Some(Email("john.doe@digital.hmrc.gov.uk", verified = true, verifiedOn = verifiedOn)), Seq())
      when(searchServiceMock.searchPreference(any())(any(), any(), any())).thenReturn(Future.successful(List(preference)))

      val result = searchController.search(addToken(FakeRequest("GET", queryParamsForValidNino).withSession(User.sessionKey -> "user")))

      status(result) shouldBe Status.OK
      private val document = Jsoup.parse(bodyOf(result).futureValue)

      document.body().getElementById("confirm").getElementsByTag("form").attr("action") shouldBe
        "/paperless/admin/search/opt-out?taxIdentifierName=nino&taxIdentifierValue=CE067583D"
    }

    "return a not found error message if the preference is not found" in new TestCase {
      when(searchServiceMock.searchPreference(any())(any(), any(), any())).thenReturn(Future.successful(Nil))
      Mockito.reset(auditConnectorMock)
      val result = searchController.search(addToken(FakeRequest("GET", queryParamsForValidNino).withSession(User.sessionKey -> "user")))

      status(result) shouldBe Status.OK
      bodyOf(result).futureValue should include ("No paperless preference found for that identifier.")
    }

    "call the search service with an uppercase taxIdentifier if a lowercase taxIdentifier is provided through the Form" in new TestCase {
      val preference = Preference(genericPaperless = true, genericUpdatedAt = genericUpdatedAt, taxCreditsPaperless = true, taxCreditsUpdatedAt = taxCreditsUpdatedAt,
        Some(Email("john.doe@digital.hmrc.gov.uk", verified = true, verifiedOn = verifiedOn)), Seq())
      when(searchServiceMock.searchPreference(any())(any(), any(), any())).thenReturn(Future.successful(List(preference)))

      val result = searchController.search(addToken(FakeRequest("GET", queryParamsForValidLowercaseNino).withSession(User.sessionKey -> "user")))

      verify(searchServiceMock, times(1)).searchPreference(ArgumentMatchers.eq(TaxIdentifier("nino", "CE067583D")))(any(), any(), any())
      verify(searchServiceMock, times(0)).searchPreference(ArgumentMatchers.eq(TaxIdentifier("nino", "ce067583d")))(any(), any(), any())
      status(result) shouldBe Status.OK
      private val document = Jsoup.parse(bodyOf(result).futureValue)

      document.body().getElementById("confirm").getElementsByTag("form").attr("action") shouldBe
        "/paperless/admin/search/opt-out?taxIdentifierName=nino&taxIdentifierValue=CE067583D"
    }
  }

  "submit opt out request" should {
    val genericUpdatedAt = Some(new DateTime(2018, 2, 15, 0, 0, DateTimeZone.UTC))
    val taxCreditsUpdatedAt = Some(new DateTime(2018, 2, 15, 0, 0, DateTimeZone.UTC))
    val verifiedOn = Some(new DateTime(2018, 2, 15, 0, 0, DateTimeZone.UTC))
    "redirect to the confirm page" in new TestCase with ScalaFutures {
      val preference = Preference(genericPaperless = true, genericUpdatedAt = genericUpdatedAt, taxCreditsPaperless = true, taxCreditsUpdatedAt = taxCreditsUpdatedAt,
        Some(Email("john.doe@digital.hmrc.gov.uk", verified = true, verifiedOn = verifiedOn)), Seq())
      when(searchServiceMock.optOut(ArgumentMatchers.eq(TaxIdentifier("nino", "CE067583D")),any())(any(), any(), any())).thenReturn(Future.successful(OptedOut))

      private val request = FakeRequest(Helpers.POST, controllers.routes.SearchController.optOut("nino", "CE067583D").url)
        .withFormUrlEncodedBody("reason" -> "my optOut reason")
        .withSession(User.sessionKey -> "user")

      val result = searchController.optOut("nino", "CE067583D")(addToken(request))

      status(result) shouldBe SEE_OTHER
      header("Location", result) shouldBe Some(controllers.routes.SearchController.confirmed("nino", "CE067583D").url)
    }
  }
}

trait TestCase extends MockitoSugar {

  implicit val appConfig = mock[FrontendAppConfig]

  when(appConfig.analyticsToken).thenReturn("")
  when(appConfig.analyticsHost).thenReturn("")

  val auditConnectorMock = mock[AuditConnector]
  when(auditConnectorMock.sendEvent(any())(any(), any())).thenReturn(Future.successful(AuditResult.Success))

  val searchServiceMock = mock[SearchService]

  def searchController()(implicit messages: MessagesApi) = new SearchController(auditConnectorMock, searchServiceMock)

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
