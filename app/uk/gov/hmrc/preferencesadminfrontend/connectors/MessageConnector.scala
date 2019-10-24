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

package uk.gov.hmrc.preferencesadminfrontend.connectors

import akka.actor.ActorSystem
import javax.inject.{Inject, Singleton}
import play.api.http.Status._
import play.api.{Configuration, Environment}
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.audit.DefaultAuditConnector
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient
import uk.gov.hmrc.preferencesadminfrontend.model._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MessageConnector @Inject()(frontendAuditConnector: DefaultAuditConnector,
                                 http: DefaultHttpClient,
                                 environment: Environment,
                                 val runModeConfiguration: Configuration,
                                 val actorSystem: ActorSystem,
                                val servicesConfig: ServicesConfig
                                )(implicit ec:ExecutionContext) {


  def serviceUrl: String = servicesConfig.baseUrl("message")

  def addRescindments(rescindmentRequest: RescindmentRequest)
                     (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[RescindmentUpdateResult] = {
    http.POST[RescindmentRequest, RescindmentUpdateResult](s"$serviceUrl/admin/message/add-rescindments", rescindmentRequest)
  }

  def sendRescindmentAlerts()
                     (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[RescindmentAlertsResult] = {
    http.POSTEmpty[RescindmentAlertsResult](s"$serviceUrl/admin/send-rescindment-alerts")
  }

  def getWhitelist()(implicit hc:HeaderCarrier): Future[HttpResponse] = {
    http.GET[HttpResponse](s"$serviceUrl/admin/message/brake/gmc/whitelist").recover {
      case e: Exception => HttpResponse(BAD_GATEWAY, None, Map(), Some(e.getMessage))
    }
  }

  def addFormIdToWhitelist(formIdEntry: WhitelistEntry)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    http.POST[WhitelistEntry,HttpResponse](s"$serviceUrl/admin/message/brake/gmc/whitelist/add",formIdEntry).recover {
      case e: Exception => HttpResponse(BAD_GATEWAY, None, Map(), Some(e.getMessage))
    }
  }

  def deleteFormIdFromWhitelist(formIdEntry: WhitelistEntry)(implicit hc:HeaderCarrier): Future[HttpResponse] = {
    http.POST[WhitelistEntry,HttpResponse](s"$serviceUrl/admin/message/brake/gmc/whitelist/delete", formIdEntry).recover {
      case e: Exception => HttpResponse(BAD_GATEWAY, None, Map(), Some(e.getMessage))
    }
  }

  def getGmcBatches()(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    http.GET[HttpResponse](s"$serviceUrl/admin/message/brake/gmc/batches").recover {
      case e: Exception => HttpResponse(BAD_GATEWAY, None, Map(), Some(e.getMessage))
    }
  }

  def getRandomMessagePreview(batch: GmcBatch)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    http.POST[GmcBatch,HttpResponse](s"$serviceUrl/admin/message/brake/random", batch).recover {
      case e: Exception => HttpResponse(BAD_GATEWAY, None, Map(), Some(e.getMessage))
    }
  }

  def approveGmcBatch(batch: GmcBatchApproval)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    http.POST[GmcBatchApproval,HttpResponse](s"$serviceUrl/admin/message/brake/accept", batch).recover {
      case e: Exception => HttpResponse(BAD_GATEWAY, None, Map(), Some(e.getMessage))
    }
  }

  def rejectGmcBatch(batch: GmcBatchApproval)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    http.POST[GmcBatchApproval,HttpResponse](s"$serviceUrl/admin/message/brake/reject", batch).recover {
      case e: Exception => HttpResponse(BAD_GATEWAY, None, Map(), Some(e.getMessage))
    }
  }

}