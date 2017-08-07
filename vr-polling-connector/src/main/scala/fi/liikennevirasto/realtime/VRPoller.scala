package fi.liikennevirasto.realtime

import java.io.InputStream

import com.google.transit.realtime.gtfsrt.FeedMessage
import com.trueaccord.scalapb.json.JsonFormat
import org.json4s.JsonAST.{JString, JValue}
import org.json4s.jackson.JsonMethods
import org.slf4j.{Logger, LoggerFactory}

import scala.io.Source

class VRPoller (publisher: Publisher) extends ParametricURLPoller[FeedMessage](publisher: Publisher){
  override protected val log: Logger = LoggerFactory.getLogger(this.getClass)

  override def fromInputStream(inputStream: InputStream): FeedMessage = {
    log.info(System.currentTimeMillis() + ": Converting to String...")
    val s = Source.fromInputStream(inputStream).mkString
    inputStream.close()
    val json = JsonMethods.parse(s)
    properties.getProperty("type") match {
      case "update" => {
        if (VRJsonToGTFSMapper.jsonToVRObjects(json).isEmpty) {
          log.info("No updated data for the last version")
          setParam("")
        } else {
          log.info("Latest version: " + VRJsonToGTFSMapper.jsonToVRObjects(json).map(_.version).max)
          setParam(VRJsonToGTFSMapper.jsonToVRObjects(json).map(_.version).max.toString)
        }
        VRJsonToGTFSMapper.passengerTrainsFromJson(json)
      }
      case "position" => {
        VRJsonToGTFSMapper.trainsPositionFromJson(json)
      }
    }
  }

  override def publish(feedMessage: FeedMessage): Unit =  {
    log.info(System.currentTimeMillis() + ": String created, publishing...")
    val message = JsonFormat.toJsonString(feedMessage)
    publisher.publish(message)
  }
}
