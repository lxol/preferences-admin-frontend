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

package uk.gov.hmrc.preferencesadminfrontend

import org.scalatest.Ignore
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.preferencesadminfrontend.connectors.EntityResolverConnector
import uk.gov.hmrc.preferencesadminfrontend.services.SearchService
import uk.gov.hmrc.preferencesadminfrontend.services.model.{Preference, TaxIdentifier}

@Ignore
class SearchServiceSpec extends UnitSpec with MockitoSugar with ScalaFutures {


  "getPreferences" should {

    val validSaUtr = TaxIdentifier("sautr", "123456789")
    val validNino = TaxIdentifier("nino", "SS123456S")

    val entityResolverConnector = mock[EntityResolverConnector]
    val searchService = new SearchService(entityResolverConnector)



    "return preferences for nino user when it exists" in {

        val result = searchService.getPreference(validSaUtr).futureValue

        result shouldBe(Right(Some(Preference)))
    }

    "return preferences for utr user when it exists" in {

    }

    "return none if the identifier does not exist" in {

    }

    "return ErrorMessage if taxIdentifier is invalid" in {

    }

    "return ErrorMEssage if something goes wrong when calling downstream dependencies" in {

    }
  }
}
