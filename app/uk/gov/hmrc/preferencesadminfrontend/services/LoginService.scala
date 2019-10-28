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

import com.google.common.io.BaseEncoding
import com.typesafe.config.ConfigException.Missing
import javax.inject.Inject
import play.api.{Configuration, Environment}
import uk.gov.hmrc.play.bootstrap.config.RunMode
import uk.gov.hmrc.preferencesadminfrontend.controllers.model.User

class LoginService @Inject()(loginServiceConfig: LoginServiceConfiguration) {

  def isAuthorised(user: User): Boolean = loginServiceConfig.authorisedUsers.contains(user)
}

class LoginServiceConfiguration @Inject()(val configuration: Configuration,
                                          val env: Environment) {

  def verifyConfiguration() = if (authorisedUsers.isEmpty) throw new Missing("Property users is empty")

  lazy val authorisedUsers: Seq[User] = {
    configuration.get[Seq[Configuration]](s"$env.users").map {
      userConfig: Configuration =>
        val encodedPwd = userConfig.get[String]("password")
        val decodedPwd = new String(BaseEncoding.base64().decode(encodedPwd))
        User(
          userConfig.get[String]("username"),
          decodedPwd
        )
    }
  }
}