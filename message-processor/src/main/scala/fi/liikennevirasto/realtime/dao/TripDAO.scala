package fi.liikennevirasto.realtime.dao

import java.sql.{Date, ResultSet}

import slick.jdbc.PostgresProfile.api._

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration.Duration
import akka.actor.ActorSystem
import com.google.transit.realtime.gtfsrt.TripUpdate
import org.slf4j.LoggerFactory

case class Trip(id: Long, externalId: String, externalShortId: String, routeId: String, headSign: Option[String],
                timetable: Seq[StopTimeTable])

case class StopTimeTable(id: Long, stopId: String, externalId: String, arrivalTime: String, departureTime: String)

case class Geolocation(latitude: Float, longitude: Float)

case class TripShortInfo(tripId:String, routeId: String, direction: Int)

object TripDAO {
  val db = DataBase
  val system = ActorSystem("save-position")
  private val log = LoggerFactory.getLogger(this.getClass)
  implicit val ec: ExecutionContext = system.dispatcher

  private val lahtiTripData =
    s"""SELECT trip_id
        FROM stop_timetables
        WHERE trip_id IN
              (SELECT trip_id
               FROM trips
               WHERE route_id = ? AND direction_id = ?)
               AND to_char(departure_time, 'HH24:mi:SS') = ? AND stop_sequence = '1';"""

  private val saveTrip =
    s"""INSERT INTO trip_updates VALUES (DEFAULT, ?, ?, ?, ?, ?, ?, ?, ?)"""

  private val saveStopTimes =
    s"""INSERT INTO stop_time_update VALUES (DEFAULT, CURRVAL('trip_updates_id_seq'), ?, ?, ?, ?, ?, ?, ?)"""

  private val getTrainTripId =
    s"""SELECT trip_id,route_id,direction_id
        FROM trips
        WHERE service_id in
          (SELECT service_id FROM operating_days
            WHERE start_date <= ?
              and end_date >= ?
          )"""

  private val oneWeekDayCode =
    s"""and trip_id like ?;"""

  private val twoWeekDayCode =
    s"""and (trip_id like ? or trip_id like ?);"""

  private val getTrainPosition=
    s"""SELECT
        section_name,
        st_y((st_dumppoints(st_transform(st_setsrid(geom, 2393), 4326))).geom) AS latitude,
        st_x((st_dumppoints(st_transform(st_setsrid(geom, 2393), 4326))).geom) AS longitude
        FROM interpol_sections
        WHERE section_name = ?;"""

  private val TWO_SECONDS = Duration.create(2, "seconds") // Wait at most this much for the query to complete

  def getTrip(connectorName: String, routeShortName: String, tripStartDate: String, scheduledTime: String): Seq[Trip] = {
    Await.result(db.withSession(
      sql"""SELECT t.id, t.trip_eid, t.trip_seid, t.route_id, t.trip_headsign,
            st.id, st.stop_id, s.stop_eid, st.stop_sequence, st.arrival_time, st.departure_time
            FROM trips t JOIN
              stop_timetables st ON (st.trip_id = t.id) JOIN operating_days od ON (od.id = t.operating_days_id) JOIN
              stops s on (s.id = st.stop_id)
            WHERE t.id = (
              SELECT t.id FROM trips t JOIN stop_timetables st ON (st.trip_id = t.id) JOIN
                operating_days od ON (od.id = t.operating_days_id) JOIN
                routes r ON (r.id = t.route_id) JOIN
                agency a ON (a.id = r.agency_id)
              WHERE connector_name = $connectorName AND route_short_name = $routeShortName AND
                start_date <= TIMESTAMPTZ $tripStartDate AND end_date >= TIMESTAMPTZ $tripStartDate AND
                (array[sunday, monday, tuesday, wednesday, thursday, friday, saturday])[(select
                  1+extract(dow from TIMESTAMPTZ $tripStartDate)::int)] AND
                 stop_sequence = 1 and st.departure_time = interval $scheduledTime
               LIMIT 1)""".as[
        (Long, String, String, String, Option[String],
          Long, String, String, String, String)]
    ), atMost = TWO_SECONDS).groupBy(x => (x._1, x._2, x._3, x._4, x._5)).map { case (x, y) =>
      Trip(x._1, x._2, x._3, x._4, x._5, y.map(sts => StopTimeTable(sts._6, sts._7, sts._8, sts._9, sts._10)))
    }.toSeq
  }

  def getTrip(connectorName: String, routeShortName: String, tripPartialId: String): Seq[Trip] = {
    val partialMatch = "_" + tripPartialId
    Await.result(db.withSession(
      sql"""SELECT t.id, t.trip_eid, t.trip_seid, t.route_id, t.trip_headsign,
            st.id, st.stop_id, s.stop_eid, st.stop_sequence, st.arrival_time, st.departure_time
            FROM trips t JOIN
              stop_timetables st ON (st.trip_id = t.id) JOIN operating_days od ON (od.service_id = t.operating_days_id) JOIN
              stops s on (s.id = st.stop_id)
            WHERE t.id = (
              SELECT t.id FROM trips t JOIN
                routes r ON (r.route_id = t.route_id) JOIN
                agency a ON (a.agency_id = r.agency_id)
              WHERE connector_name = $connectorName AND route_short_name = $routeShortName AND
                trip_seid = $partialMatch
               LIMIT 1)""".as[
        (Long, String, String, String, Option[String],
          Long, String, String, String, String)]
    ), atMost = TWO_SECONDS).groupBy(x => (x._1, x._2, x._3, x._4, x._5)).map { case (x, y) =>
      Trip(x._1, x._2, x._3, x._4, x._5, y.map(sts => StopTimeTable(sts._6, sts._7, sts._8, sts._9, sts._10)))
    }.toSeq
  }

  def getTrip(routeId: String, directionId: Int, startTime: String): Option[String] = {
    val connection = db.createConnection
    var rs = None: Option[ResultSet]
    var trip = None: Option[String]
    rs = Some(db.withPreparedStatement(connection, lahtiTripData, Seq(routeId, directionId, startTime)))
    rs match {
      case Some(result) => {
        while (result.next) {
          trip = Some(result.getString("trip_id"))
        }
        result.close
      }
      case None =>
    }
    connection.close
    trip
  }

  def saveTripUpdate(id: String, tripUpdate: TripUpdate): Unit = {
    val connection = db.createConnection
    try {
      db.insertWithPreparedStatement(
        connection,
        saveTrip,
        Seq(
          id,
          tripUpdate.getTimestamp,
          tripUpdate.trip.getTripId,
          tripUpdate.trip.getRouteId,
          tripUpdate.trip.getDirectionId,
          tripUpdate.trip.getStartDate,
          tripUpdate.trip.getStartTime,
          tripUpdate.getVehicle.getId))

      tripUpdate
        .stopTimeUpdate
        .zipWithIndex.foreach{
        case (upd, i) => {
          db.insertWithPreparedStatement(
            connection,
            saveStopTimes,
            Seq(
              upd.getStopId,
              i,
              upd.getStopSequence,
              upd.getArrival.getTime,
              upd.getArrival.getDelay,
              upd.getDeparture.getTime,
              upd.getDeparture.getDelay
            )
          )
        }
      }
    } catch {
      case ex: Exception => log.error(s"Error saving TripUpdate $id", ex)
    } finally {
      connection.close
    }
  }

  def getTrainTripId(trainType: String, weekDay: String, date: String, time: String): Option[TripShortInfo] = {
    val connection = db.createConnection
    val sqlDate = Date.valueOf(date)
    val dayCodes = weekDay.split("/")
    var rs = None: Option[ResultSet]
    var trip = None: Option[TripShortInfo]
    dayCodes.length match {
      case 1 =>rs = Some(db.withPreparedStatement(connection, getTrainTripId + oneWeekDayCode, Seq(sqlDate, sqlDate, s"%${trainType}_%_${dayCodes(0)}_%_$time")))
      case 2 =>rs = Some(db.withPreparedStatement(connection, getTrainTripId + twoWeekDayCode, Seq(sqlDate, sqlDate, s"%${trainType}_%_${dayCodes(0)}_%_$time", s"%${trainType}_%_${dayCodes(1)}_%_$time")))
    }
    rs match {
      case Some(result) => {
        while (result.next) {
          trip = Some(TripShortInfo(result.getString("trip_id"), result.getString("route_id"), result.getInt("direction_id")))
        }
        result.close
      }
      case None =>
    }
    connection.close
    trip
  }

  def getTrainPosition(trackName:String): Option[Geolocation] = {
    val connection = db.createConnection
    var rs = None: Option[ResultSet]
    var geo = None: Option[Geolocation]
    rs = Some(db.withPreparedStatement(connection, getTrainPosition, Seq(trackName)))
    rs match {
      case Some(result) => {
        while (result.next) {
          geo = Some(Geolocation(result.getFloat("latitude"), result.getFloat("longitude")))
        }
        result.close
      }
      case None =>
    }
    connection.close
    geo
  }
}