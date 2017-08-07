package fi.liikennevirasto.realtime.strategy

import akka.actor.{ActorSystem, Props}
import com.google.transit.realtime.gtfsrt.{Position, TripDescriptor, VehiclePosition}
import fi.liikennevirasto.realtime.{MessageActor, RealTimeMessage, VehicleDetails}
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._

trait VehiclePositionStrategy {

  private val topicPrefix = "/hfp/journey"
  implicit val system = ActorSystem("save-position")
  system.eventStream.subscribe(system.actorOf(Props[MessageActor]), classOf[VehicleDetails])

  def handleMessage(connectorName: String, vehiclePosition: VehiclePosition): RealTimeMessage = {
    val vp = fillVehiclePositionData(vehiclePosition)
    system.eventStream.publish(VehicleDetails(connectorName.toUpperCase,vp))
    val msg = toDigitransitJson(vp, connectorName)
    RealTimeMessage(connectorName, msg.getBytes, getTopic(vp))
  }

  def getTopic(vehiclePosition: VehiclePosition): String = {
    getVehicleTopic(vehiclePosition, vehicleMode)
  }

  def getVehicleTopic(vehiclePosition: VehiclePosition, vType: String): String = {
    s"$topicPrefix" +
      s"/$vType" +
      s"/${vehiclePosition.getVehicle.getId}" +
      s"/${vehiclePosition.getTrip.getRouteId}" +
      s"/${vehiclePosition.getTrip.getDirectionId}" +
      s"/XXX" + //headsign, doesn't come in gtfs
      s"/${vehiclePosition.getTrip.getStartTime}" +
      s"/${vehiclePosition.getStopId}"
  }

  def fillVehiclePositionData(vehiclePosition: VehiclePosition): VehiclePosition

  def getTripData(tripDescriptor: TripDescriptor): TripDescriptor

  def getPositionData(vehiclePosition: VehiclePosition): Position

  def vehicleMode: String

  def toDigitransitJson(vehiclePosition: VehiclePosition, connectorName: String): String = {
    val json =
      ("VP" ->
        ("id" -> vehiclePosition.getVehicle.getId) ~
          ("route" -> vehiclePosition.getTrip.getRouteId) ~
          ("operator" -> "XXX") ~
          ("direction" -> vehiclePosition.getTrip.getDirectionId) ~
          ("start_time" -> vehiclePosition.getTrip.getStartTime) ~
          ("oday" -> vehiclePosition.getTrip.getStartDate) ~
          ("mode" -> vehicleMode) ~
          ("dl" -> 0) ~
          ("next_stop" -> vehiclePosition.getStopId) ~
          ("stop_index" -> vehiclePosition.getCurrentStopSequence) ~
          ("tsi" -> vehiclePosition.getTimestamp) ~
          ("lat" -> vehiclePosition.getPosition.latitude) ~
          ("long" -> vehiclePosition.getPosition.longitude) ~
          ("hdg" -> vehiclePosition.getPosition.getBearing) ~
          ("odometer" -> vehiclePosition.getPosition.getOdometer) ~
          ("speed" -> vehiclePosition.getPosition.getSpeed) ~
          ("source" -> connectorName.toUpperCase))
    compact(render(json))
  }
}
