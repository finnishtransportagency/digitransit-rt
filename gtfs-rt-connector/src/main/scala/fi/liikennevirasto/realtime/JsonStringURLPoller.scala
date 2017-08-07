package fi.liikennevirasto.realtime

import java.io.InputStream

import org.slf4j.{Logger, LoggerFactory}

import scala.io.Source

class JsonStringURLPoller(publisher: Publisher) extends URLPoller[String](publisher: Publisher) {
  override protected val log: Logger =  LoggerFactory.getLogger(this.getClass)

  override def fromInputStream(inputStream: InputStream): String = {
    log.info(System.currentTimeMillis() + ": Converting to String...")
    Source.fromInputStream(inputStream).mkString("\n")
  }

  override def publish(message: String): Unit =  {
    log.info(System.currentTimeMillis() + ": String created, publishing...")
    publisher.publish(message)
  }
}
