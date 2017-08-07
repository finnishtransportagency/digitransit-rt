package fi.liikennevirasto.realtime.strategy

import java.text.SimpleDateFormat
import java.util.Calendar

import com.google.transit.realtime.gtfsrt.{Position, TripDescriptor, VehiclePosition}
import fi.liikennevirasto.realtime._
import fi.liikennevirasto.realtime.dao.{TripDAO, TripShortInfo}

/**
  *
  */
object VrVehiclePosition extends VehiclePositionStrategy {

  private val dateFormat = new SimpleDateFormat("yyyyMMdd")

  override def fillVehiclePositionData(vehiclePosition: VehiclePosition): VehiclePosition = {
    vehiclePosition
      .withTrip(getTripData(vehiclePosition.getTrip))
      .withPosition(getPositionData(vehiclePosition))
      .withTimestamp(System.currentTimeMillis() / 1000)
  }

  override def getTripData(tripDescriptor: TripDescriptor): TripDescriptor = {
    tripDescriptor.getTripId match {
      case "" => tripDescriptor
      case _ => fillTripDescriptor(tripDescriptor)
    }
  }

  override def getPositionData(vehiclePosition: VehiclePosition): Position = {
    val geo = TripDAO.getTrainPosition(vehiclePosition.getStopId)
    geo match {
      case Some(g) => {
        vehiclePosition
          .getPosition
          .withLatitude(g.latitude)
          .withLongitude(g.longitude)
      }
      case None => vehiclePosition.getPosition
    }
  }

  override def vehicleMode: String = "rail"

  private def fillTripDescriptor(tripDescriptor: TripDescriptor): TripDescriptor = {
    getTripId(tripDescriptor.getTripId) match {
      case Some(tripInfo) => tripDescriptor
        .withTripId(tripInfo.tripId)
        .withRouteId(tripInfo.routeId.split("_")(1))
        .withDirectionId(tripInfo.direction + 1)
        .withStartDate(dateFormat.format(Calendar.getInstance().getTime))
        .withStartTime("0000")
      case None => TripDescriptor.defaultInstance
    }
  }

  private def getTripId(composedTripId: String): Option[TripShortInfo] = {
    val vrData = composedTripId.split("_")
    TripDAO
      .getTrainTripId(
        vrData(0),
        convertWeekDay(vrData(0),vrData(1)),
        vrData(2),
        vrData(3))
  }
}
