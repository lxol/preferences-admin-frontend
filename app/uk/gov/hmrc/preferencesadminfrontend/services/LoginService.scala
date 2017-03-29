package uk.gov.hmrc.preferencesadminfrontend.services

import uk.gov.hmrc.preferencesadminfrontend.controllers.model.User

class LoginService(authorisedUsers: Seq[User]) {
  def login(user: User) = true
}
