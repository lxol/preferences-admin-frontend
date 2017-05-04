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

import org.joda.time.{DateTime, DateTimeZone, Duration}
import play.api.mvc.{Filter, RequestHeader, Result}
import uk.gov.hmrc.play.filters.MicroserviceFilterSupport
import uk.gov.hmrc.play.filters.frontend.SessionTimeoutFilter

import scala.concurrent.Future

class SessionTimeoutFilterWithEntryPoint(clock: () => DateTime = () => DateTime.now(DateTimeZone.UTC),
                                         timeoutDuration: Duration,
                                         additionalSessionKeysToKeep: Set[String] = Set.empty,
                                         onlyWipeAuthToken: Boolean = false)
  extends SessionTimeoutFilter(clock, timeoutDuration, additionalSessionKeysToKeep, onlyWipeAuthToken) with Filter with MicroserviceFilterSupport {

  lazy val entryPoint: String = uk.gov.hmrc.preferencesadminfrontend.controllers.routes.LoginController.showLoginPage().path

  override def apply(f: (RequestHeader) => Future[Result])(rh: RequestHeader): Future[Result] = {
    if (rh.path == entryPoint) f(rh)
    else super.apply(f)(rh)
  }
}
