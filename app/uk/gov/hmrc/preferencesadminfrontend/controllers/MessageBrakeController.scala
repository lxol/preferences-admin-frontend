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

package uk.gov.hmrc.preferencesadminfrontend.controllers

import javax.inject.Inject
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Request, Result}
import uk.gov.hmrc.play.frontend.controller.FrontendController
import uk.gov.hmrc.preferencesadminfrontend.config.AppConfig
import uk.gov.hmrc.preferencesadminfrontend.connectors.MessageConnector
import uk.gov.hmrc.preferencesadminfrontend.model.GmcBatch
import uk.gov.hmrc.preferencesadminfrontend.services.MessageService
import uk.gov.hmrc.preferencesadminfrontend.views.html.{error_template, message_brake_admin}

class MessageBrakeController @Inject()(messageConnector: MessageConnector, messageService: MessageService)
                                      (implicit appConfig: AppConfig, val messagesApi: MessagesApi) extends FrontendController with I18nSupport {

  def showAdminPage: Action[AnyContent] = AuthorisedAction.async {
    implicit request =>
      implicit user =>
        messageService.getGmcBatches.map {
          case Left(batches) => Ok(message_brake_admin(batches))
          case Right(error) => returnError(error)
        }
  }

  def previewMessage:  Action[AnyContent] = AuthorisedAction.async {
    implicit request =>
      implicit user =>
        val batch = GmcBatch().bindFromRequest().get
        for {
          gmcBatches <- messageService.getGmcBatches()
          messagePreview <- messageService.getRandomMessagePreview(batch)
        } yield {
          gmcBatches match {
            case Left(batches) => messagePreview match {
              case Left(preview) => Ok(message_brake_admin(batches, Some(preview)))
              case Right(error) => returnError(error)
            }
            case Right(error) => returnError(error)
          }
        }
  }

  def approveBatch: Action[AnyContent] = AuthorisedAction.async {
    implicit request =>
      implicit user =>
        val batch = GmcBatch().bindFromRequest().get
        for {
          result <- messageConnector.approveGmcBatch(batch)
          batchesResult <- messageService.getGmcBatches()
        } yield {
          result.status match {
            case OK => batchesResult match {
              case Left(batches) => Ok(message_brake_admin(batches))
              case Right(error) => returnError(error)
            }
            case _ => returnError("Failed to approve batch.")
          }
        }
  }

  def rejectBatch: Action[AnyContent] = AuthorisedAction.async {
    implicit request =>
      implicit user =>
        val batch = GmcBatch().bindFromRequest().get
        for {
          result <- messageConnector.rejectGmcBatch(batch)
          batchesResult <- messageService.getGmcBatches()
        } yield {
          result.status match {
            case OK => batchesResult match {
              case Left(batches) => Ok(message_brake_admin(batches))
              case Right(error) => returnError(error)
            }
            case _ => returnError("Failed to reject batch.")
          }
        }
  }

  private def returnError(error: String)(implicit request: Request[_]): Result = {
    BadGateway(error_template("Error", "There was an error:", error, appConfig))
  }

}