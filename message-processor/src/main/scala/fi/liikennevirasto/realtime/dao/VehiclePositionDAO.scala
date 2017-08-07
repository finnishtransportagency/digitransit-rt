package fi.liikennevirasto.realtime.dao

import java.sql.ResultSet

import com.google.transit.realtime.gtfsrt.VehiclePosition
import org.slf4j.LoggerFactory

case class VehiclePositionData(stopId: String, currentStop: Int)

case class TripData(startTime: String, tripId: String, routeId: String, directionId: Int)


object VehiclePositionDAO {

  private val db = DataBase
  private val log = LoggerFactory.getLogger(this.getClass)
  private val getOuluTripData =
    s"""SELECT
          st.start_time,
          t.trip_id,
          t.route_id,
          t.direction_id
        FROM (SELECT
                trip_id,
                service_id,
                route_id,
                direction_id
              FROM trips
              WHERE
                 trip_id = ?) AS t
        LEFT JOIN (
          SELECT
            to_char(departure_time, 'HH24MI') AS start_time,
            trip_id
          FROM stop_timetables
          WHERE
            trip_id = ?
            AND stop_sequence = 0) AS st
          ON st.trip_id = t.trip_id LIMIT 1"""

  private val stopIdQuery = s"""SELECT stop_id FROM stops WHERE stop_code = ?"""

  private val savePositionQuery = s"""INSERT INTO vehicle_positions VALUES (DEFAULT, ?, ?, ?, ?, ?, ?, ?)"""

  private val wgs84FromGeo =
    s"""SELECT
          section_name,
          st_y((st_dumppoints(st_transform(st_setsrid(geom, 2393), 4326))).geom) AS latitude,
          st_x((st_dumppoints(st_transform(st_setsrid(geom, 2393), 4326))).geom) AS longitude
        FROM interpol_sections
        WHERE section_name = ?;"""


  private val getLahtiStopId =
    s"""SELECT stop_id
        FROM stop_time_update
        WHERE
          trip_update_id =
          (SELECT max(id)
           FROM trip_updates
           WHERE
             timestamp =
             (SELECT max(timestamp)
              FROM trip_updates
              WHERE
                vehicle_id = ?)
             AND trip_id in
                 (SELECT trip_id
                  FROM stop_timetables
                  WHERE trip_id IN
                        (SELECT trip_id
                         FROM trips
                         WHERE
                           route_id = ?) AND
                        to_char(departure_time, 'HH24:mi:SS') = ? AND stop_sequence = '1')) AND update_sequence = 0 LIMIT 1;"""

  def getOuluTripData(tripId: String): Option[TripData] = {
    val connection = db.createConnection
    var rs = None: Option[ResultSet]
    var trip = None: Option[TripData]
    rs = Some(db.withPreparedStatement(connection, getOuluTripData, Seq(tripId, tripId)))
    rs match {
      case Some(result) => {
        while (result.next) {
          trip = Some(TripData(result.getString("start_time"), result.getString("trip_id"), result.getString("route_id"), result.getInt("direction_id")))
        }
        result.close
      }
      case None => {
        trip = Some(TripData("0000", "0", "0", 1))
      }
    }
    connection.close
    trip
  }

  def getStopId(stopCode: String): Option[String] = {
    val connection = db.createConnection
    var rs = None: Option[ResultSet]
    var stopId = None: Option[String]
    rs = Some(db.withPreparedStatement(connection, stopIdQuery, Seq(stopCode)))
    rs match {
      case Some(result) => {
        while (result.next) {
          stopId = Some(result.getString("stop_id"))
        }
        result.close
      }
      case None => {
        stopId = Some(stopCode)
      }
    }
    connection.close
    stopId
  }

  def saveVehiclePosition(feedOrigin: String, vehiclePosition: VehiclePosition): Unit = {
    val connection = db.createConnection
    try {
      db.insertWithPreparedStatement(
        connection,
        savePositionQuery,
        Seq(
          vehiclePosition.getTimestamp,
          feedOrigin,
          vehiclePosition.getTrip.getTripId,
          vehiclePosition.getTrip.getRouteId,
          vehiclePosition.getVehicle.getId,
          vehiclePosition.getPosition.latitude,
          vehiclePosition.getPosition.longitude))
    } catch {
      case ex: Exception => log.error(s"Error saving vehicle position from $feedOrigin", ex)
    } finally {
      connection.close
    }
  }

  def getLahtiStopId(vehicleId: String, routeId: String, startTime: String): Option[String] = {
    val connection = db.createConnection
    var rs = None: Option[ResultSet]
    var stopId = None: Option[String]
    rs = Some(db.withPreparedStatement(connection, getLahtiStopId, Seq(vehicleId, routeId, startTime)))
    rs match {
      case Some(result) => {
        while (result.next) {
          stopId = Some(result.getString("stop_id"))
        }
        result.close
      }
      case None =>
    }
    connection.close
    stopId
  }

}
