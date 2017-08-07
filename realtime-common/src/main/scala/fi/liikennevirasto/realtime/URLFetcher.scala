package fi.liikennevirasto.realtime

import java.io.InputStream
import java.util.Base64

import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClientBuilder
import org.slf4j.{Logger, LoggerFactory}

case class Credentials(userName: String, password: String)

/**
  * Class for basic HTTP with authentication purposes
  */
class URLFetcher(credentials: Option[Credentials]) {

  private lazy val httpClient = HttpClientBuilder.create().build()
  val log: Logger =  LoggerFactory.getLogger(this.getClass)

  val BASIC_AUTH = "Basic"
  val AUTHORIZATION = "Authorization"

  def urlConnection(url: String): InputStream = {
    val get = new HttpGet(url)
    if (credentials.nonEmpty) {
      get.setHeader(AUTHORIZATION, BASIC_AUTH + " " + Base64.getEncoder.encodeToString((credentials.get.userName + ":" + credentials.get.password).getBytes))
    }
    val result = httpClient.execute(get)
    if (result.getStatusLine.getStatusCode >= 400) {
      throw new RuntimeException("Invalid HTTP Response")
    } else {
      val httpEntity = result.getEntity
      val contentType = httpEntity.getContentType.getValue
      if (contentType != "application/x-protobuf" && contentType.startsWith("application/json"))
        log.warn(s"Invalid content-Type received ${httpEntity.getContentType.getValue}")
      httpEntity.getContent
    }
  }
}
