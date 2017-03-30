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
import com.google.inject.Inject
import org.slf4j.MDC
import play.api.Configuration
import play.api.mvc.{RequestHeader, Result}
import uk.gov.hmrc.play.http.logging.filters.LoggingFilter
import uk.gov.hmrc.preferencesadminfrontend.config.ControllerConfiguration

import scala.concurrent.Future

class MicroserviceLoggingFilter @Inject()(
  conf: Configuration,
  controllerConf: ControllerConfiguration
)(implicit val mat: Materializer) extends LoggingFilter {

  private lazy val appName = conf.getString("appName").getOrElse("APP NAME NOT SET")
  private lazy val loggerDateFormat = conf.getString("logger.json.dateformat")

  override def apply(next: (RequestHeader) => Future[Result])(rh: RequestHeader): Future[Result] = {
    MDC.put("appName", appName)
    loggerDateFormat.foreach(str => MDC.put("logger.json.dateformat", str))
    super.apply(next)(rh)
  }

  override def controllerNeedsLogging(controllerName: String): Boolean = {
    controllerConf.getControllerConfig(controllerName).needsLogging
  }

}
