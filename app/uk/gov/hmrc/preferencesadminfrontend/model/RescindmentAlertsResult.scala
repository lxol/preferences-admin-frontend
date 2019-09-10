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

package uk.gov.hmrc.preferencesadminfrontend.model

import play.api.libs.json.{Reads, __}
import play.api.libs.functional.syntax._

case class RescindmentAlertsResult(sent: Int, requeued: Int, failed: Int, hardCopyRequested: Int)

object RescindmentAlertsResult {
  implicit val reads: Reads[RescindmentAlertsResult] = (
    (__ \ "alerts sent").read[Int] and
      (__ \ "ready for retrial").read[Int] and
      (__ \ "failed permanently").read[Int] and
      (__ \ "hard copy requested").read[Int]
    )(RescindmentAlertsResult.apply _)
}