package fi.liikennevirasto.realtime.strategy

import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.{Locale, TimeZone}

import akka.actor.ActorSystem
import com.google.transit.realtime.gtfsrt.{FeedEntity, FeedMessage, TripDescriptor}
import fi.liikennevirasto.realtime._
import fi.liikennevirasto.realtime.TripDetails
import fi.liikennevirasto.realtime.dao.TripDAO
import org.joda.time.DateTimeZone
import org.joda.time.format.DateTimeFormat
import org.json4s.NoTypeHints
import org.json4s.jackson.JsonMethods.parse
import org.json4s.jackson.Serialization

/**
  *
  */
object ConvertVrTripIds extends ForwardStrategy {

  implicit val formats = Serialization.formats(NoTypeHints) + new LongSerializer()
  implicit val system = ActorSystem("save-trip")

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
    val trainData = trip.getTripId.split("_")
    val trainType:String = trainData(0)
    val weekDay:String = trainData(1)
    val date:String = trainData(2)
    val time:String = trainData(3)
    val tripId = TripDAO.getTrainTripId(trainType, convertWeekDay(trainType,weekDay),date, time)
    tripId match {
      case Some(tripInfo) => trip
          .withTripId(tripInfo.tripId)
          .withRouteId(tripInfo.routeId.split("_")(1))
      case None => trip
    }
  }

}
