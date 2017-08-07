package fi.liikennevirasto.realtime.strategy

import java.nio.charset.Charset

import akka.actor.{ActorSystem, Props}
import com.google.transit.realtime.gtfsrt.{FeedEntity, FeedMessage, TripDescriptor}
import fi.liikennevirasto.realtime.{MessageActor, TripDetails}
import fi.liikennevirasto.realtime.dao.TripDAO
import org.json4s.JsonAST.{JLong, JString}
import org.json4s.{CustomSerializer, NoTypeHints}
import org.json4s.jackson.JsonMethods.parse
import org.json4s.jackson.Serialization
import org.slf4j.LoggerFactory

/**
  * Converts trip ID's in the lahti feed to match the datasource
  */
object ConvertTripIds extends ForwardStrategy {

  private val log = LoggerFactory.getLogger(this.getClass)
  implicit val formats = Serialization.formats(NoTypeHints) + new LongSerializer()
  implicit val system = ActorSystem("save-trip")
  system.eventStream.subscribe(system.actorOf(Props[MessageActor]), classOf[TripDetails])

  override def handleMessage(connectorName: String, payload: Array[Byte]): Array[Byte] = {
    val feedMessage = parse(new String(payload, Charset.defaultCharset())).extract[FeedMessage]
    val newEntities = feedMessage.entity.map(convertEntity(connectorName))
    system.eventStream.publish(TripDetails(newEntities))
    feedMessage.withEntity(newEntities).toByteArray
  }

  private def convertEntity(connectorName: String)(entity: FeedEntity): FeedEntity = {
    entity.tripUpdate match {
      case Some(upd) => {
        entity.withTripUpdate(upd.withTrip(getTripData(connectorName, upd.trip)))
      }
      case _ => entity
    }
  }

  private def getTripData(connectorName: String, trip: TripDescriptor): TripDescriptor = {
    val tripId = TripDAO.getTrip(connectorName.toUpperCase + "_" + trip.getRouteId, trip.getDirectionId, trip.getStartTime)
    tripId match {
      case Some(id) => trip
        .withTripId(id)
      case None => trip
    }
  }
}
