package fi.liikennevirasto.realtime.strategy

import java.text.SimpleDateFormat
import java.util.Calendar

import com.google.transit.realtime.gtfsrt.{Position, TripDescriptor, VehiclePosition}

/*
*
* Jyvaskyla vehicle position strategy
*
* */
object JyvaskylaVehiclePosition extends VehiclePositionStrategy {

  private val dateFormat = new SimpleDateFormat("yyyyMMdd")

  override def fillVehiclePositionData(vehiclePosition: VehiclePosition): VehiclePosition = {
    addRouteLabel(
      vehiclePosition
        .withStopId(vehiclePosition.getStopId)
        .withCurrentStopSequence(vehiclePosition.getCurrentStopSequence)
        .withTrip(getTripData(vehiclePosition.getTrip))
        .withPosition(getPositionData(vehiclePosition)))
  }

  override def getTripData(tripDescriptor: TripDescriptor): TripDescriptor = {
    tripDescriptor
      .withDirectionId(1) // Its always 0 in the database
      .withStartDate(dateFormat.format(Calendar.getInstance().getTime))
      .withStartTime("0000")
  }

  override def getPositionData(vehiclePosition: VehiclePosition): Position = {
    vehiclePosition
      .getPosition
        .withOdometer(0)
        .withSpeed(0)
        .withBearing(0)
  }

  override def vehicleMode: String = "bus"

  def addRouteLabel(vehiclePosition: VehiclePosition): VehiclePosition = {
    vehiclePosition
      .withTrip(vehiclePosition
        .getTrip
        .withRouteId(s"${vehiclePosition.getVehicle.getLabel}_${vehiclePosition.getTrip.getRouteId}"))
  }
}
