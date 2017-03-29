package uk.gov.hmrc.preferencesadminfrontend

import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.preferencesadminfrontend.controllers.model.User
import uk.gov.hmrc.preferencesadminfrontend.services.LoginService

class LoginServiceSpec extends UnitSpec {

  "login" should {
    "allow an authorised user into the system" in new TestCase {
      lazy val loginService = new LoginService(authorisedUsers)
      val user = new User("username", "password")

      loginService.login(user) shouldBe true
    }
  }

  trait TestCase {
    val authorisedUsers = Seq(new User("username", "password"))
  }
}
