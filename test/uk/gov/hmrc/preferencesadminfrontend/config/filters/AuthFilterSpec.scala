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

package uk.gov.hmrc.preferencesadminfrontend.config.filters

import akka.stream.Materializer
import org.joda.time.DateTime
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Configuration
import play.api.mvc.{RequestHeader, Result, Session}
import uk.gov.hmrc.play.http.SessionKeys
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.time.DateTimeUtils

import scala.concurrent.Future

class AuthFilterSpec extends UnitSpec with ScalaFutures with GuiceOneAppPerSuite {

  "AuthFilter" should {
    "continue to the next filter if path is restricted and user is in a non expired session" in new TestCase {

      val validRequestHeader = mock[RequestHeader]
      val validSession = Session() + ("user", "testUser") + (SessionKeys.lastRequestTimestamp, DateTimeUtils.now.getMillis.toString)
      when(validRequestHeader.session).thenReturn(validSession)
      when(validRequestHeader.path).thenReturn("/paperless/admin/search")

      val result = filter.apply(nextFilter)(validRequestHeader).futureValue

      result.header.status shouldBe 200
    }

    "redirect to login page if path is restricted and user is not in session" in new TestCase {
      val validRequestHeader = mock[RequestHeader]
      val emptySession = new Session()
      when(validRequestHeader.session).thenReturn(emptySession)
      when(validRequestHeader.path).thenReturn("/paperless/admin/search")

      val result = filter.apply(nextFilter)(validRequestHeader).futureValue

      result.header.status shouldBe 303
    }

    "continue to the next filter if path is no restricted and session is not expired" in new TestCase {
      val validRequestHeader = mock[RequestHeader]
      val validSession = Session(Map(SessionKeys.lastRequestTimestamp -> DateTimeUtils.now.getMillis.toString))
      when(validRequestHeader.session).thenReturn(validSession)
      when(validRequestHeader.path).thenReturn("/paperless/admin")

      val result = filter.apply(nextFilter)(validRequestHeader).futureValue

      result.header.status shouldBe 200
    }
  }

  "AuthFilter.userDefinedIn" should {
    "return true if the user is defined in the session" in {
      AuthFilter.userDefinedIn(Session(Map("user" -> "user"))) shouldBe true
    }

    "return false if the user is not defined in the session" in {
      AuthFilter.userDefinedIn(Session()) shouldBe false
    }
  }

  "AuthFilter.isExpired" should {
    "return true if timestamp is older than sessionTimeoutMillis" in {
      val sessionTimeoutMillis = 60 * 1000
      AuthFilter.isExpired(
        Session(Map(SessionKeys.lastRequestTimestamp -> DateTimeUtils.now.minusMinutes(2).getMillis.toString)),
        sessionTimeoutMillis
      ) shouldBe true
    }

    "return false if timestamp is newer than sessionTimeoutMillis" in {
      val sessionTimeoutMillis = 120 * 1000
      AuthFilter.isExpired(
        Session(Map(SessionKeys.lastRequestTimestamp -> DateTimeUtils.now.minusMinutes(1).getMillis.toString)),
        sessionTimeoutMillis
      ) shouldBe false
    }

    "return false if timestamp is not available in session" in {
      val sessionTimeoutMillis = 120 * 1000
      AuthFilter.isExpired(
        Session(),
        sessionTimeoutMillis
      ) shouldBe true
    }
  }

  "AuthFilter.updateTimestampFor" should {
    lazy val currentTime: DateTime = DateTimeUtils.now

    val dateTimeUtils: DateTimeUtils = new DateTimeUtils {
      override lazy val now: DateTime = currentTime
    }

    "add a timestamp when not present in session" in {
      AuthFilter.updateTimestampFor(Session(), dateTimeUtils).data should contain(SessionKeys.lastRequestTimestamp -> currentTime.getMillis.toString)
    }

    "update a timestamp when present in session" in {
      AuthFilter.updateTimestampFor(Session(Map(SessionKeys.lastRequestTimestamp -> currentTime.minusMinutes(1).getMillis.toString)), dateTimeUtils).data should contain(SessionKeys.lastRequestTimestamp -> currentTime.getMillis.toString)
    }
  }

  trait TestCase extends MockitoSugar {

    import scala.concurrent.ExecutionContext.Implicits.global

    implicit val materializer = mock[Materializer]

    val defaultTimeout = 5000

    val configuration = {
      val myMock = mock[Configuration]
      when(myMock.getInt("userSessionTimeoutInMillis")).thenReturn(Some(defaultTimeout))
      myMock
    }

    val filter = new AuthFilter(configuration)

    def nextFilter: (RequestHeader) => Future[Result] = (r: RequestHeader) => Future.successful(play.api.mvc.Results.Ok)
  }

}
