package fi.liikennevirasto.realtime

import java.text.SimpleDateFormat
import java.util.SimpleTimeZone

import com.google.transit.realtime.gtfsrt.Alert.Cause
import com.google.transit.realtime.gtfsrt.FeedHeader.Incrementality
import com.google.transit.realtime.gtfsrt.TripDescriptor.ScheduleRelationship
import com.google.transit.realtime.gtfsrt.TripUpdate.{StopTimeEvent, StopTimeUpdate}
import com.google.transit.realtime.gtfsrt._
import fi.liikennevirasto.realtime.Category._
import fi.liikennevirasto.realtime.vr.{CauseCategorySerializer, TimeTableRow, VRObject}
import org.joda.time.DateTimeZone
import org.joda.time.format.ISODateTimeFormat
import org.json4s.JsonAST.JValue
import org.json4s.{DefaultFormats, Formats, StringInput}
import org.json4s.jackson.{Json, JsonMethods}

object VRWebSocketConnector extends App with WebSocketConnector{

  override def handleMessage(message: String): Unit = {
    val stringInput = StringInput(message)
    JsonMethods.parse(stringInput)
    println(message)
  }
}

object VRJsonToGTFSMapper {
  def VROBJECT_TYPE_REGULAR = "REGULAR"
  def VROBJECT_TYPE_ADHOC = "ADHOC"

  def VROBJECT_STOPTYPE_ARRIVAL = "ARRIVAL"
  def VROBJECT_STOPTYPE_DEPARTURE = "DEPARTURE"

  implicit val jsonFormats: Formats = new DefaultFormats {
    override def dateFormatter = new SimpleDateFormat("yyyy-MM-dd")
  } + CauseCategorySerializer
  def fromJson(jValue: JValue): FeedMessage = {
    vrObjectToFeedMessage(jsonToVRObjects(jValue))
  }

  private def jsonToVRObjects(jValue: JValue): List[VRObject] = {
    jValue.extract[List[VRObject]]
  }

  private def vrObjectToFeedMessage(vrObjects: List[VRObject]): FeedMessage = {
    val message = FeedMessage.defaultInstance
    message
      .withHeader(header(message))
      .withEntity(feedEntities(message, vrObjects))
  }

  private def header(message: FeedMessage): FeedHeader = {
    val header = message.header
    header
      .withGtfsRealtimeVersion("1.0")
      .withTimestamp(System.currentTimeMillis())
      .withIncrementality(Incrementality.FULL_DATASET)

  }

  private def feedEntities(message: FeedMessage, vrObjects: List[VRObject]): Seq[FeedEntity] = {
    vrObjects.map(feedEntity)
  }

  private def feedEntity(vrObject: VRObject): FeedEntity = {
    val al = alert(vrObject)
    val tripUpd = FeedEntity.defaultInstance.withId(vrObject.version.toString).withTripUpdate(tripUpdate(vrObject))
    al.map(tripUpd.withAlert).getOrElse(tripUpd)
  }

  private def tripUpdate(obj: VRObject): TripUpdate = {
    TripUpdate.defaultInstance
      .withTrip(tripDescriptor(obj))
      .withStopTimeUpdate(stopTimeUpdate(obj))
      .withVehicle(vehicleDescriptor(obj))
  }
  //
  //  private def vehicle(obj: VRObject): VehiclePosition = {
  //  }
  //
  private def tripDescriptor(obj: VRObject): TripDescriptor = {
    TripDescriptor.defaultInstance
      .withRouteId(if (obj.commuterLineID.getOrElse("") == "")
        s"${obj.operatorShortCode}_${obj.trainType}${obj.trainNumber}"
      else
        s"${obj.trainType}-${obj.commuterLineID.get}")
      .withTripId(s"${obj.operatorShortCode}_${obj.trainType}${obj.trainNumber}")
      .withStartTime(extractTime(obj.timeTableRows.head.scheduledTime))
      .withStartDate(extractDate(obj.timeTableRows.head.scheduledTime))
      .withScheduleRelationship(
        if (obj.timetableType == VROBJECT_TYPE_REGULAR)
          ScheduleRelationship.SCHEDULED
        else
          ScheduleRelationship.ADDED
      )
  }

  private def vehicleDescriptor(obj: VRObject): VehicleDescriptor = {
    // We don't have engine numbers or other ways of telling apart the vehicles
    VehicleDescriptor.defaultInstance
  }
  private def stopTimeUpdate(obj: VRObject): Seq[StopTimeUpdate] = {
    val (arrivals, departures) = obj.timeTableRows.filter(t => t.trainStopping || t.cancelled)
      .partition(_.`type` == VROBJECT_STOPTYPE_ARRIVAL)
    val sortedArrivals = arrivals.sortBy(a => toUnixTime(a.scheduledTime))
    val sortedDepartures = departures.sortBy(d => toUnixTime(d.scheduledTime))
    Seq(departureStopTimeUpdate(sortedDepartures.head)) ++
    sortedArrivals.zip(sortedDepartures.tail).zipWithIndex.map(x => stopTimeUpdate(x)) ++
    Seq(arrivalStopTimeUpdate(sortedArrivals.last, sortedArrivals.size + 1))
  }

  private def departureStopTimeUpdate(departure: TimeTableRow) = {
    val scheduledTimeDep = toUnixTime(departure.scheduledTime)
    val estimatedTimeDep = departure.liveEstimateTime.map(toUnixTime)
    StopTimeUpdate.defaultInstance
      .withStopId(departure.stationShortCode) // TODO: Map to static data stop ids
      // TODO: uncertainty values
      .withDeparture(stopTimeEvent(estimatedTimeDep.map(est => (est - scheduledTimeDep).toInt), estimatedTimeDep, None))
      .withScheduleRelationship(scheduleRelationship(true, departure.cancelled))
      .withStopSequence(1)
  }

  private def arrivalStopTimeUpdate(arrival: TimeTableRow, index: Int) = {
    val scheduledTimeArr = toUnixTime(arrival.scheduledTime)
    val estimatedTimeArr = arrival.liveEstimateTime.map(toUnixTime)
    StopTimeUpdate.defaultInstance
      .withStopId(arrival.stationShortCode) // TODO: Map to static data stop ids
      // TODO: uncertainty values
      .withArrival(stopTimeEvent(estimatedTimeArr.map(est => (est - scheduledTimeArr).toInt), estimatedTimeArr, None))
      .withScheduleRelationship(scheduleRelationship(arrival.trainStopping, arrival.cancelled))
      .withStopSequence(index)
  }

  private def stopTimeUpdate(obj: ((TimeTableRow, TimeTableRow), Int)): StopTimeUpdate = {
    val ((arrival, departure), index) = obj
    assert(arrival.stationShortCode == departure.stationShortCode)
    val scheduledTimeArr = toUnixTime(arrival.scheduledTime)
    val estimatedTimeArr = arrival.liveEstimateTime.map(toUnixTime)
    val scheduledTimeDep = toUnixTime(departure.scheduledTime)
    val estimatedTimeDep = departure.liveEstimateTime.map(toUnixTime)
    StopTimeUpdate.defaultInstance
      .withStopId(arrival.stationShortCode) // TODO: Map to static data stop ids
      // TODO: uncertainty values
      .withArrival(stopTimeEvent(estimatedTimeArr.map(est => (est - scheduledTimeArr).toInt), estimatedTimeArr, None))
      .withDeparture(stopTimeEvent(estimatedTimeDep.map(est => (est - scheduledTimeDep).toInt), estimatedTimeDep, None))
      .withScheduleRelationship(scheduleRelationship(arrival.trainStopping, arrival.cancelled))
      .withStopSequence(index + 1) // We handle departure separately
  }

  private def stopTimeEvent(delay: Option[Int], time: Option[Long], uncertainty: Option[Int]): StopTimeEvent = {
    (time,delay,uncertainty) match {
      case (Some(t), Some(d), Some(u)) => StopTimeEvent.defaultInstance.withTime(t).withDelay(d).withUncertainty(u)
      case (None, Some(d), Some(u)) => StopTimeEvent.defaultInstance.withDelay(d).withUncertainty(u)
      case (Some(t), None, Some(u)) => StopTimeEvent.defaultInstance.withTime(t).withUncertainty(u)
      case (Some(t), Some(d), None) => StopTimeEvent.defaultInstance.withTime(t).withDelay(d)
      case (None, None, Some(u)) => StopTimeEvent.defaultInstance.withUncertainty(u)
      case (Some(t), None, None) => StopTimeEvent.defaultInstance.withTime(t)
      case (None, Some(d), None) => StopTimeEvent.defaultInstance.withDelay(d)
      case (None, None, None) => StopTimeEvent.defaultInstance
    }
  }

  private def scheduleRelationship(stopping: Boolean, cancelled: Boolean): StopTimeUpdate.ScheduleRelationship = {
    if (!stopping || cancelled)
      StopTimeUpdate.ScheduleRelationship.SKIPPED
    else
      StopTimeUpdate.ScheduleRelationship.SCHEDULED
  }

  // VR: Syy -> Alert
  // TODO: Check which alerts are new. Now assuming the latest information is new

  //TODO: This isn't currently working at all FIXME
  private def alert(obj: VRObject): Option[Alert] = {
    obj.timeTableRows.filter(t => t.actualTime.isEmpty && t.causes.nonEmpty)
      .flatMap(ttr =>
        ttr.causes.lastOption.map( c =>
          c.detailedCategory.flatMap(_.overrideCause).map( cc =>
            Alert.defaultInstance
            .withActivePeriod(Seq(TimeRange.defaultInstance.withEnd(toUnixTime(ttr.scheduledTime))))
            .withCause(cc))
      )
      ).lastOption.flatten
  }

  def extractDate(isoDateTime: String):String = {
    val sdf = new SimpleDateFormat("YYYYMMDD")
    sdf.setTimeZone(new SimpleTimeZone(SimpleTimeZone.UTC_TIME, "UTC"))
    sdf.format(ISODateTimeFormat.dateTimeParser().parseDateTime(isoDateTime).toDate)
  }
  def extractTime(isoDateTime: String):String = {
    val sdf = new SimpleDateFormat("HH:mm:ss")
    sdf.setTimeZone(new SimpleTimeZone(SimpleTimeZone.UTC_TIME, "UTC"))
    sdf.format(ISODateTimeFormat.dateTimeParser().parseDateTime(isoDateTime).toDate)
  }

  /**
    * Convert ISO date time to Unix timestamp
    * @param isoDateTime Date time in ISO format (e.g. "2017-02-21T06:47:03.000Z")
    * @return
    */
    def toUnixTime(isoDateTime: String):Long = {
    ISODateTimeFormat.dateTimeParser().parseDateTime(isoDateTime).toDate.getTime / 1000
  }
}

