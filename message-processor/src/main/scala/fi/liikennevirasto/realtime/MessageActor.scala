package fi.liikennevirasto.realtime

import akka.actor.Actor
import com.google.transit.realtime.gtfsrt.{FeedEntity, VehiclePosition}
import fi.liikennevirasto.realtime.dao.{TripDAO, VehiclePositionDAO}
import org.slf4j.LoggerFactory

/**
  * Actor that saves data from the received messages
  */
class MessageActor extends Actor {

  private val log = LoggerFactory.getLogger(this.getClass)

  override def receive: Receive = {
    case tripDetails: TripDetails => saveTripUpdate(tripDetails)
    case vehicleDetails: VehicleDetails => saveVehiclePosition(vehicleDetails)
    case _ => log.error("Invalid Message")
  }

  private def saveTripUpdate(tripDetails: TripDetails): Unit = {
    tripDetails.entities.map {
      ent => TripDAO.saveTripUpdate(ent.id, ent.getTripUpdate)
    }
  }

  private def saveVehiclePosition(vehicleDetails: VehicleDetails): Unit = {
    VehiclePositionDAO.saveVehiclePosition(
      vehicleDetails.source,
      vehicleDetails.vehiclePosition
    )
  }
}

case class TripDetails(entities: Seq[FeedEntity])

case class VehicleDetails(source:String, vehiclePosition: VehiclePosition)