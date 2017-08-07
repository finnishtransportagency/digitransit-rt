package fi.liikennevirasto.realtime.strategy

import java.nio.charset.Charset

import com.google.transit.realtime.gtfsrt.TripUpdate.StopTimeUpdate
import com.google.transit.realtime.gtfsrt.{FeedEntity, FeedMessage, TripDescriptor, TripUpdate}
import fi.liikennevirasto.realtime.dao.{DataBase, Trip, TripDAO}
import org.json4s.DefaultFormats
import org.json4s.jackson.JsonMethods.parse
import org.slf4j.LoggerFactory

object ConvertStopIds extends ForwardStrategy{

  private val log = LoggerFactory.getLogger(this.getClass)
  implicit val formats = DefaultFormats

  override def handleMessage(connectorName: String, payload: Array[Byte]): Array[Byte] = {
    val jsonString = new String(payload, Charset.defaultCharset())
    val feedMessage = parse(jsonString).extract[FeedMessage]
    val newEntities = feedMessage.entity.map(convertEntity(connectorName))
    feedMessage.withEntity(newEntities).toByteArray
  }

  private def convertEntity(connectorName: String)(entity: FeedEntity): FeedEntity = {
    entity.tripUpdate match {
      case Some(upd) => {
        val staticData = getStaticTripData(connectorName, upd.trip)
        log.info("Read static data for trip %s ".format(upd.trip.getTripId) + staticData)
        if (staticData.isEmpty)
          log.info("Not found for trip with connector %s, route %s, tripId %s because not all values are presentt".format(connectorName, upd.trip.routeId, upd.trip.tripId))
        entity.withTripUpdate(convertTripUpdate(staticData)(upd))
      }
      case _ => entity
    }
  }

  private def convertTripUpdate(trip: Option[Trip])(tripUpdate: TripUpdate): TripUpdate = {
    tripUpdate
      .withTrip(convertTrip(trip, tripUpdate.trip))
      .withStopTimeUpdate(tripUpdate.stopTimeUpdate.map(x => convertStop(trip, x).getOrElse(x)))
  }

  private def getStaticTripData(connectorName: String, trip: TripDescriptor): Option[Trip] = {
    try {
      val routeShortName = trip.routeId
      val tripShortId = trip.tripId
      (routeShortName, tripShortId) match {
          // TODO: remove this test
        case (Some(route), Some(id)) => TripDAO.getTrip(connectorName, route, id).headOption
        case _ =>
          log.warn("No trip searched for connector %s, route %s, tripId %s because not all values are present".format(connectorName, routeShortName, tripShortId))
          None
      }

    } catch {
      case ex: Exception => log.warn("Exception when finding a trip", ex)
        None
    }

  }

  private def convertStop(trip: Option[Trip], stop: StopTimeUpdate): Option[StopTimeUpdate] = {
    trip.map(t => stop.withStopId(t.timetable.find(_.externalId.endsWith("_"+stop.stopId.getOrElse(""))).map(_.externalId).getOrElse(stop.stopId.getOrElse(""))))
  }

  private def convertTrip(trip: Option[Trip], tripDescriptor: TripDescriptor): TripDescriptor = {
    tripDescriptor.withRouteId(trip.map(_.routeId.toString).getOrElse(tripDescriptor.getRouteId))
      .withTripId(trip.map(_.externalId).getOrElse(tripDescriptor.getTripId))
  }
}
