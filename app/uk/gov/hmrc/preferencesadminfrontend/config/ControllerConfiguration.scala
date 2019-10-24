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

// /*
//  * Copyright 2019 HM Revenue & Customs
//  *
//  * Licensed under the Apache License, Version 2.0 (the "License");
//  * you may not use this file except in compliance with the License.
//  * You may obtain a copy of the License at
//  *
//  *     http://www.apache.org/licenses/LICENSE-2.0
//  *
//  * Unless required by applicable law or agreed to in writing, software
//  * distributed under the License is distributed on an "AS IS" BASIS,
//  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  * See the License for the specific language governing permissions and
//  * limitations under the License.
//  */

// package uk.gov.hmrc.preferencesadminfrontend.config

// import com.google.inject.Inject
// import play.api.Configuration

// class ControllerConfiguration @Inject()(conf: Configuration) {

//   val rootConfig = "controllers"

//   def getControllerConfig(controllerName: String): ControllerParams = {
//     val controller = s"$rootConfig.$controllerName"
//     ControllerParams(
//       needsLogging = conf.getBoolean(s"$controller.needsLogging").getOrElse(true),
//       needsAuditing = conf.getBoolean(s"$controller.needsAuditing").getOrElse(true)
//     )
//   }
// }

// case class ControllerParams(needsLogging: Boolean = true, needsAuditing: Boolean = true)
