package uk.gov.hmrc.preferencesadminfrontend.config.filters

import akka.stream.Materializer
import uk.gov.hmrc.play.test.UnitSpec
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import play.api.mvc.{Filter, RequestHeader, Result, Session}

import scala.concurrent.Future

class AuthFilterSpec extends UnitSpec with ScalaFutures {

  "AuthFilterSpec.apply" should {
    "continue to the next filter if path is restricted and user is in session" in new TestCase {
      val validRequestHeader = mock[RequestHeader]
      val validSession = new Session() + ("user" -> "testUser")
      when(validRequestHeader.session).thenReturn(validSession)
      when(validRequestHeader.path).thenReturn("/search")

      val result = filter.apply(nextFilter)(validRequestHeader).futureValue

      result.header.status shouldBe 200
    }

    "redirect to login page if path is restricted and user is not in session" in new TestCase {
      val validRequestHeader = mock[RequestHeader]
      val validSession = new Session()
      when(validRequestHeader.session).thenReturn(validSession)
      when(validRequestHeader.path).thenReturn("/search")

      val result = filter.apply(nextFilter)(validRequestHeader).futureValue

      result.header.status shouldBe 303
    }

    "continue to the next filter if path is no restricted" in new TestCase {
      val validRequestHeader = mock[RequestHeader]
      val validSession = new Session()
      when(validRequestHeader.session).thenReturn(validSession)
      when(validRequestHeader.path).thenReturn("/")

      val result = filter.apply(nextFilter)(validRequestHeader).futureValue

      result.header.status shouldBe 200
    }

  }

  trait TestCase extends MockitoSugar {
    import scala.concurrent.ExecutionContext.Implicits.global
    implicit val materializer = mock[Materializer]

    val filter = new AuthFilter()
    def nextFilter: (RequestHeader) => Future[Result] = (r: RequestHeader) => Future.successful(play.api.mvc.Results.Ok)
  }
}
