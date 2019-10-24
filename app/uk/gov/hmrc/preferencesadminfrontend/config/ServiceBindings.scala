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

package uk.gov.hmrc.preferencesadminfrontend.config

import play.api.inject.{Binding, Module}
import play.api.{Configuration, Environment}
import uk.gov.hmrc.preferencesadminfrontend.config.filters.PreferencesFrontendAuditFilter

class ServiceBindings extends Module {
    // override def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] =
    //   bindDeps ++ bindConnectors ++ bindServices ++ bindControllers ++ bindFilters
    override def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] =
        bindFilters

    // private def bindDeps: Seq[Binding[_]] = Seq(
    //   bind[AuthorisedAction].to[AuthorisedActionImpl].eagerly()
    // )

    private def bindFilters: Seq[Binding[_]] = Seq(
        bind[PreferencesFrontendAuditFilter].toSelf.eagerly())

    // )

    // private def bindConnectors: Seq[Binding[_]] = Seq(
    //   bind[EnrolmentActivationPrintingConnector].to[EnrolmentActivationPrintingConnectorImpl].eagerly(),
    //   bind[EnrolmentEventStoreConnector].to[EnrolmentEventStoreConnectorImpl].eagerly(),
    //   bind[EnrolmentStoreProxyConnector].to[EnrolmentStoreProxyConnectorImpl].eagerly(),
    //   bind[SECConnector].to[SECConnectorImpl].eagerly(),
    //   bind[UsersGroupsSearchConnector].to[UsersGroupsSearchConnectorImpl].eagerly(),
    //   bind[GroupSyncOrchestratorConnector].to[GroupSyncOrchestratorConnectorImpl].eagerly()
    // )

    // private def bindServices: Seq[Binding[_]] = Seq(
    //   bind[AuditService].to[AuditServiceImpl].eagerly(),
    //   bind[EnrolmentActivationPrintingService].to[EnrolmentActivationPrintingServiceImpl].eagerly(),
    //   bind[EnrolmentStoreProxyService].to[EnrolmentStoreProxyServiceImpl].eagerly(),
    //   bind[AWSS3FileUploadService].to[AWSS3FileUploadServiceImpl].eagerly()
    // )

    // private def bindControllers: Seq[Binding[_]] = Seq(
    //   bind[AgentCodeController].toSelf.eagerly(),
    //   bind[EESController].toSelf.eagerly(),
    //   bind[EnrolmentActivationPrintingController].toSelf.eagerly(),
    //   bind[ESPController].toSelf.eagerly(),
    //   bind[HomeController].toSelf.eagerly(),
    //   bind[LoginController].toSelf.eagerly(),
    //   bind[RemoveDupeEnrolmentsController].toSelf.eagerly(),
    //   bind[RemoveKnownFactsConfirmationController].toSelf.eagerly(),
    //   bind[RemoveKnownFactsController].toSelf.eagerly(),
    //   bind[SupportController].toSelf.eagerly(),
    //   bind[UGSController].toSelf.eagerly(),
    //   bind[GroupSyncOrchestratorController].toSelf.eagerly(),
    //   bind[DeleteNonAgentPrincipalEnrolmentsController].toSelf.eagerly()
    // )
}
