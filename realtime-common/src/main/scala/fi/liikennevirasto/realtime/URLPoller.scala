package fi.liikennevirasto.realtime

import java.io.InputStream

import akka.actor.Actor
import org.slf4j.Logger

abstract class URLPoller[A](publisher: Publisher) extends Actor {
  protected val log: Logger

  private val credentials: Option[Credentials] =
    if (properties.getProperty("source.user") != "" && properties.getProperty("source.pass") != "") {
      Some(Credentials(properties.getProperty("source.user"), properties.getProperty("source.pass")))
    } else {
      None
    }

  protected lazy val fetcher = new URLFetcher(credentials)

  override def receive: Actor.Receive = {
    case source: String => pollSourceAndPublish(source)
    case x => log.warn("Received unhandled message " + x)
  }

  def pollSourceAndPublish(source: String): Unit = {
    publish(fromInputStream(pollSource(source)))
  }

  def pollSource(source: String): InputStream = {
    log.info(System.currentTimeMillis() + ": Connecting to: " + source)
    fetcher.urlConnection(source)
  }

  def fromInputStream(inputStream: InputStream): A

  def publish(feedMessage: A): Unit
}
