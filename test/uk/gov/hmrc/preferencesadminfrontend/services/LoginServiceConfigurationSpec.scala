/*
 * Copyright 2019 HM Revenue & Customs
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

package uk.gov.hmrc.preferencesadminfrontend.services

import com.google.common.base.Charsets
import com.google.common.io.BaseEncoding
import com.typesafe.config.ConfigException.Missing
import org.scalatest.mockito.MockitoSugar
import play.api.{Configuration, Environment}
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.preferencesadminfrontend.controllers.model.User

class LoginServiceConfigurationSpec extends UnitSpec {

  "authorisedUsers" should {

    "return a Seq of users if the configuration is valid for a single user with encoded password" in new TestCase {
      val mapUser = Map(
        "username" -> user.username,
        "password" -> BaseEncoding.base64().encode(user.password.getBytes(Charsets.UTF_8))
      )

      val loginServiceConfiguration = new LoginServiceConfiguration(configurationForUsers(mapUser), mockEnvironment)

      loginServiceConfiguration.authorisedUsers.size shouldBe 1
      loginServiceConfiguration.authorisedUsers shouldBe Seq(user)
    }

    "return a Seq of users if the configuration is valid for multiple users with encoded password" in new TestCase {
      val mapUser = Map(
        "username" -> user.username,
        "password" -> BaseEncoding.base64().encode(user.password.getBytes(Charsets.UTF_8))
      )

      val loginServiceConfiguration = new LoginServiceConfiguration(configurationForUsers(mapUser, mapUser), mockEnvironment)

      loginServiceConfiguration.authorisedUsers.size shouldBe 2
      loginServiceConfiguration.authorisedUsers shouldBe Seq(user,user)
    }

    "throw a Missing exception if the users configuration is missing" in new TestCase {
      val loginServiceConfiguration = new LoginServiceConfiguration(Configuration.from(Map.empty),mockEnvironment)

      val caught = intercept[Missing](loginServiceConfiguration.authorisedUsers)
      caught.getMessage should include("Property users missing")
    }

    "throw a Missing exception if the username configuration is missing for a user" in new TestCase {
      val mapConfiguration = Map(
        "password" -> user.password
      )
      val loginServiceConfiguration = new LoginServiceConfiguration(configurationForUsers(mapConfiguration), mockEnvironment)

      val caught = intercept[Missing](loginServiceConfiguration.authorisedUsers)
      caught.getMessage should include("Property username missing")
    }

    "throw a Missing exception if the password configuration is missing for a user" in new TestCase {
      val mapConfiguration = Map(
        "username" -> user.username
      )
      val loginServiceConfiguration = new LoginServiceConfiguration(configurationForUsers(mapConfiguration), mockEnvironment)

      val caught = intercept[Missing](loginServiceConfiguration.authorisedUsers)
      caught.getMessage should include("Property password missing")
    }

  }

  "verifyConfiguration" should {

    "throw a Missing exception if users Seq is empty" in new TestCase {
      val loginServiceConfiguration = new LoginServiceConfiguration(Configuration.from(Map("Test.users"-> Seq.empty)), mockEnvironment)

      val caught = intercept[Missing](loginServiceConfiguration.verifyConfiguration())
      caught.getMessage should include("Property users is empty")
    }

  }

  trait TestCase extends MockitoSugar {

    val user = User("user", "pwd")
    val mockEnvironment = Environment.simple()

    def configurationForUsers(usersMap: Map[String,Any]*) = Configuration.from(Map("Test.users"-> usersMap))
  }
}
