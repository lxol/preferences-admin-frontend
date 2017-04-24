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

import akka.stream.Materializer
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.{any, argThat}
import org.mockito.Mockito.when
import org.mockito.{ArgumentMatcher, ArgumentMatchers, Mockito}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Configuration
import play.api.http.Status
import play.api.i18n.MessagesApi
import play.api.libs.json.Json
import play.api.test.{FakeRequest, Helpers}
import play.api.test.Helpers.{headers, _}
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.preferencesadminfrontend.config.FrontendAppConfig
import uk.gov.hmrc.preferencesadminfrontend.controllers.SearchController
import uk.gov.hmrc.preferencesadminfrontend.controllers.model.User
import uk.gov.hmrc.preferencesadminfrontend.services._
import uk.gov.hmrc.preferencesadminfrontend.services.model.{Email, Preference, TaxIdentifier}
import uk.gov.hmrc.preferencesadminfrontend.utils.CSRFTest

import scala.concurrent.Future


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
    val queryParamsForInvalidNino = "?name=nino&value=1234567"

    "return a preference if tax identifier exists" in new TestCase {

      val preference = Preference(paperless = true, Some(Email("john.doe@digital.hmrc.gov.uk", verified = true)), Seq())
      when(searchServiceMock.getPreference(any())(any(), any())).thenReturn(Future.successful(Some(preference)))

      val result = searchController.search(addToken(FakeRequest("GET", queryParamsForValidNino).withSession(User.sessionKey -> "user")))

      status(result) shouldBe Status.OK
      bodyOf(result).futureValue should include ("john.doe@digital.hmrc.gov.uk")

      val expectedAuditEvent = searchController.createSearchEvent("user", TaxIdentifier("nino", "CE067583D"), Some(preference))
      Mockito.verify(auditConnectorMock).sendEvent(argThat(isSimilar(expectedAuditEvent)))(any(), any())
    }


    "include a hidden form to opt the user out" in new TestCase {

      val preference = Preference(paperless = true, Some(Email("john.doe@digital.hmrc.gov.uk", verified = true)), Seq())
      when(searchServiceMock.getPreference(any())(any(), any())).thenReturn(Future.successful(Some(preference)))

      val result = searchController.search(addToken(FakeRequest("GET", queryParamsForValidNino).withSession(User.sessionKey -> "user")))

      status(result) shouldBe Status.OK
      private val document = Jsoup.parse(bodyOf(result).futureValue)

      document.body().getElementById("confirm").attr("style") shouldBe "display:none;"
      document.body().getElementById("confirm").getElementsByTag("form").attr("action") shouldBe
        "/paperless/admin/search/opt-out?taxIdentifierName=nino&taxIdentifierValue=CE067583D"

    }

    "return a not found error message if the preference is not found" in new TestCase {
      when(searchServiceMock.getPreference(any())(any(), any())).thenReturn(Future.successful(None))
      Mockito.reset(auditConnectorMock)
      val result = searchController.search(addToken(FakeRequest("GET", queryParamsForValidNino).withSession(User.sessionKey -> "user")))

      status(result) shouldBe Status.OK
      bodyOf(result).futureValue should include ("No paperless preference found for that identifier.")

      val expectedAuditEvent = searchController.createSearchEvent("user", TaxIdentifier("nino", "CE067583D"), None)
      Mockito.verify(auditConnectorMock).sendEvent(argThat(isSimilar(expectedAuditEvent)))(any(), any())
    }

  }

  "createSearchEvent" should {
    "generate the correct event when the preference exists" in new TestCase {
      val preference = Preference(paperless = true, email = Some(Email(address = "john.doe@digital.hmrc.gov.uk", verified = true)), taxIdentifiers = Seq(TaxIdentifier("sautr", "123"),TaxIdentifier("nino", "ABC")))
      val event = searchController.createSearchEvent("me", TaxIdentifier("sautr", "123"), Some(preference))

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
      val event = searchController.createSearchEvent("me", TaxIdentifier("sautr", "123"), None)

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

  "submit opt out request" should {
    "redirect to the confirm page" in new TestCase with ScalaFutures {
      val preference = Preference(paperless = true, Some(Email("john.doe@digital.hmrc.gov.uk", verified = true)), Seq())
      when(searchServiceMock.optOut(ArgumentMatchers.eq(TaxIdentifier("nino", "CE067583D")))(any(), any())).thenReturn(Future.successful(true))

      val result = searchController.optOut("nino", "CE067583D")(addToken(
        FakeRequest(Helpers.POST, controllers.routes.SearchController.optOut("nino", "CE067583D").url).withSession(User.sessionKey -> "user")))

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
