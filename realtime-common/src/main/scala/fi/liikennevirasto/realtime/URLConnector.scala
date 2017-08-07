package fi.liikennevirasto.realtime

import org.slf4j.Logger

trait URLConnector {
  protected val DEFAULT_REFRESH = 2000L
  protected val DEFAULT_PORT = "1883"

  protected val connectorId: String = properties.getProperty("connector")
  protected val source: String = properties.getProperty("source")
  protected val destination: String = properties.getProperty("destination")
  protected val port: String = properties.getProperty("port", DEFAULT_PORT)
  protected val topic: String = properties.getProperty("topic")
  protected val refreshIntervalString: String = properties.getProperty("refresh.interval", DEFAULT_REFRESH.toString)
  protected val refreshInterval: Long = if (refreshIntervalString.toLong > 0) refreshIntervalString.toLong else DEFAULT_REFRESH

  def shutdown(log: Logger, message: String, connector: String): Unit = {
    if (message != "") log.info(message)
    log.info(s"<$connector> stopped")
  }
}
