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

package uk.gov.hmrc.play.bootstrap.filters

import akka.stream.Materializer
import javax.inject.{Inject, Singleton}
import org.slf4j.MDC
import play.api.Configuration
import play.api.mvc.{Filter, RequestHeader, Result}
import uk.gov.hmrc.http.HeaderNames
import uk.gov.hmrc.play.HeaderCarrierConverter

import scala.concurrent.Future

@Singleton
class MDCFilter @Inject()(val mat: Materializer, config: Configuration) extends Filter {

  private val appName: String            = config.underlying.getString("appName")
  private val dateFormat: Option[String] = config.getString("logger.json.dateformat")

  private val extras: Set[(String, String)] = Set(
    Some("appName"                          -> appName),
    dateFormat.map("logger.json.dateformat" -> _)
  ).flatten

  override def apply(f: RequestHeader => Future[Result])(rh: RequestHeader): Future[Result] = {

    val hc =
      HeaderCarrierConverter.fromHeadersAndSession(rh.headers, Some(rh.session))

    val data = Set(
      hc.requestId.map(HeaderNames.xRequestId    -> _.value),
      hc.sessionId.map(HeaderNames.xSessionId    -> _.value),
      hc.forwarded.map(HeaderNames.xForwardedFor -> _.value)
    ).flatten ++ extras

    data.foreach {
      case (k, v) =>
        MDC.put(k, v)
    }

    f(rh)
  }
}
