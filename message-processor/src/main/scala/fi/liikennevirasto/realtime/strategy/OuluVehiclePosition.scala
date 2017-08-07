package fi.liikennevirasto.realtime.strategy

import java.text.SimpleDateFormat
import java.util.Calendar

import com.google.transit.realtime.gtfsrt.{Position, TripDescriptor, VehiclePosition}
import fi.liikennevirasto.realtime.dao.VehiclePositionDAO
import org.slf4j.LoggerFactory

/**
  * Strategy for the Oulu vehicle position feed, gets the missing data from the database and adds it to the message
  */

object OuluVehiclePosition extends VehiclePositionStrategy {

  private val log = LoggerFactory.getLogger(this.getClass)
  private val dateFormat = new SimpleDateFormat("yyyyMMdd")

  override def fillVehiclePositionData(vehiclePosition: VehiclePosition): VehiclePosition = {
    vehiclePosition
        .withTrip(getTripData(vehiclePosition.getTrip))
        .withPosition(getPositionData(vehiclePosition))
        .withTimestamp(System.currentTimeMillis() / 1000)
        .withStopId(getStopId(vehiclePosition.getStopId))
  }

  override def getTripData(tripDescriptor: TripDescriptor): TripDescriptor = {
    val tripData = VehiclePositionDAO.getOuluTripData(tripDescriptor.getTripId)
    tripData match {
      case Some(td) => tripDescriptor
          .withStartDate(dateFormat.format(Calendar.getInstance().getTime))
          .withStartTime(td.startTime)
          .withDirectionId(td.directionId + 1)
      case None => {
        log.info(s"No trip was found with ID ${tripDescriptor.getTripId}")
        tripDescriptor
      }
    }
  }

  override def getPositionData(vehiclePosition: VehiclePosition): Position = {
    vehiclePosition
      .getPosition
        .withBearing(0)
        .withOdometer(0)
        .withSpeed(0)
  }

  override def vehicleMode: String = "bus"

  private def getStopId(stopCode: String): String = {
    VehiclePositionDAO.getStopId(stopCode) match {
      case Some(stopId) => stopId
      case None => stopCode
    }
  }

}

