package fi.liikennevirasto.realtime.strategy
import java.text.SimpleDateFormat

import com.google.transit.realtime.gtfsrt.{Position, TripDescriptor, VehiclePosition}
import fi.liikennevirasto.realtime.dao.VehiclePositionDAO

/**
  * Strategy for the Lahti vehicle position feed, gets the missing data from the database and adds it to the message
  */
object LahtiVehiclePosition extends VehiclePositionStrategy {

  private val timeFormat = new SimpleDateFormat("HHmm")

  override def fillVehiclePositionData(vehiclePosition: VehiclePosition): VehiclePosition = {
    vehiclePosition
      .withTrip(getTripData(vehiclePosition.getTrip))
      .withPosition(getPositionData(vehiclePosition))
      .withTimestamp(System.currentTimeMillis() / 1000)
      .withCurrentStopSequence(0)
      .withStopId(getStopInfo(vehiclePosition))
  }

  override def getTripData(tripDescriptor: TripDescriptor): TripDescriptor = {
    tripDescriptor
      .withDirectionId(tripDescriptor.getDirectionId + 1)
      .withStartTime(convertTime(tripDescriptor.getStartTime))
  }

  override def getPositionData(vehiclePosition: VehiclePosition): Position = {
    vehiclePosition
      .getPosition
        .withOdometer(0)
  }

  override def vehicleMode: String = "bus"

  private def convertTime(time: String): String = {
    val feedTime= new SimpleDateFormat("HH:mm:ss").parse(time)
    timeFormat.format(feedTime)
  }

  private def getStopInfo(vehiclePosition: VehiclePosition): String = {
    val stopId = VehiclePositionDAO
      .getLahtiStopId(
        vehiclePosition.getVehicle.getId,
        s"LAHTI_${vehiclePosition.getTrip.getRouteId}",
        vehiclePosition.getTrip.getStartTime
      )
    stopId match {
      case Some(stop) => stop
      case None => "0000"
    }
  }
}
