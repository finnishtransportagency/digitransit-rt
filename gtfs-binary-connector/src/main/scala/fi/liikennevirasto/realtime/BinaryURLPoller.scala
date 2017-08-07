package fi.liikennevirasto.realtime

import java.io.InputStream

import com.google.transit.realtime.gtfsrt.FeedMessage
import com.trueaccord.scalapb.json.JsonFormat
import org.slf4j.{Logger, LoggerFactory}

class BinaryURLPoller(publisher: Publisher) extends URLPoller[FeedMessage](publisher: Publisher) {
  override protected val log: Logger =  LoggerFactory.getLogger(this.getClass)

  override def fromInputStream(inputStream: InputStream): FeedMessage = {
    log.info(System.currentTimeMillis() + ": Converting to object...")
    FeedMessage.parseFrom(inputStream)
  }

  override def publish(feedMessage: FeedMessage): Unit =  {
    log.info(System.currentTimeMillis() + ": Object created, converting to JSON...")

    val message = JsonFormat.toJsonString(feedMessage)
    publisher.publish(message)
  }
}
