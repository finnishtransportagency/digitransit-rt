package fi.liikennevirasto.realtime

import java.text.SimpleDateFormat
import java.util.{Calendar, Date, SimpleTimeZone, TimeZone}

import akka.actor.{Cancellable, Props}
import com.google.transit.realtime.gtfsrt.FeedHeader.Incrementality
import com.google.transit.realtime.gtfsrt.TripDescriptor.ScheduleRelationship
import com.google.transit.realtime.gtfsrt.TripUpdate.{StopTimeEvent, StopTimeUpdate}
import com.google.transit.realtime.gtfsrt._
import fi.liikennevirasto.realtime.vr._
import org.joda.time.format.ISODateTimeFormat
import org.json4s.JsonAST.JValue
import org.json4s.jackson.JsonMethods
import org.json4s.{DefaultFormats, Formats}
import org.slf4j.LoggerFactory

import scala.concurrent.duration.{FiniteDuration, MILLISECONDS}
import scala.io.Source

object VRJsonConnector extends App with URLConnector {

  private val log = LoggerFactory.getLogger(this.getClass)
  private var cancellable: Cancellable = _

  val system = akka.actor.ActorSystem("system")

  import system.dispatcher

  sys.addShutdownHook {
    shutdown()
  }

  if (!properties.isEmpty) {

    if (connectorId == "" || source == "" || destination == "" || topic == "") {
      shutdown("Missing required properties (connector, source, destination, topic)")
    }

    log.info("All properties loaded")
    val publisher = new PublisherImpl(destination, port, connectorId, topic)
    val pollPublisher = system.actorOf(Props(new VRPoller(publisher)))
    system.eventStream.subscribe(pollPublisher, classOf[String])
    cancellable = system.scheduler.schedule(FiniteDuration.apply(0L, MILLISECONDS),
      FiniteDuration.apply(refreshInterval, MILLISECONDS))(system.eventStream.publish(source))
  } else {
    shutdown("Properties are not set")
  }

  def shutdown(message: String = "", connector: String = mqttPublisher): Unit = {
    super.shutdown(log, message, connector)
    if (cancellable != null)
      cancellable.cancel()
    system.shutdown()
  }
}

object VRJsonToGTFSMapper {

  val system = akka.actor.ActorSystem("teste")

  def VROBJECT_TYPE_REGULAR = "REGULAR"
  def VROBJECT_TYPE_ADHOC = "ADHOC"

  def VROBJECT_STOPTYPE_ARRIVAL = "ARRIVAL"
  def VROBJECT_STOPTYPE_DEPARTURE = "DEPARTURE"

  def VROBJECT_CATEGORY_PASSENGER_COMMUTER = "Commuter"
  def VROBJECT_CATEGORY_PASSENGER_LONG_DISTANCE = "Long-distance"

  def VROBJECT_CATEGORY_PASSENGER = Set(VROBJECT_CATEGORY_PASSENGER_COMMUTER, VROBJECT_CATEGORY_PASSENGER_LONG_DISTANCE)

  implicit val jsonFormats: Formats = new DefaultFormats {
    override def dateFormatter = new SimpleDateFormat("yyyy-MM-dd")
  } + CauseCategorySerializer
  def passengerTrainsFromJson(jValue: JValue): FeedMessage = {
    vrObjectToFeedMessage(jsonToVRObjects(jValue).filter(o => VROBJECT_CATEGORY_PASSENGER.contains(o.trainCategory) && o.commuterLineID.getOrElse("") != ""))
  }

  def trainsPositionFromJson(jValue: JValue): FeedMessage = {
    trainTrackingToFeedMessage(jsonToTrainTracking(jValue).filter( o => compareDateTime(o.timestamp)))
  }

  /**
    * Form VRObjects from Json, including non-interesting categories (Cargo, Locomotive, On-track machines, Shunting)
    * @param jValue
    * @return
    */
  def jsonToVRObjects(jValue: JValue): List[VRObject] = {
    jValue.extract[List[VRObject]]
  }

  def jsonToTrainTracking(jValue: JValue): List[TrainTracking] = {
    jValue.extract[List[TrainTracking]]
  }

  private def vrObjectToFeedMessage(vrObjects: List[VRObject]): FeedMessage = {
    val message = FeedMessage.defaultInstance
    message
      .withHeader(header(message))
      .withEntity(feedEntitiesUpdates(message, vrObjects))
  }
  private def trainTrackingToFeedMessage(trainsTracking: List[TrainTracking]): FeedMessage = {
    val message = FeedMessage.defaultInstance
    message
      .withHeader(header(message))
      .withEntity(feedEntitiesPositions(message, trainsTracking))
  }

  private def header(message: FeedMessage): FeedHeader = {
    val header = message.header
    header
      .withGtfsRealtimeVersion("1.0")
      .withTimestamp(System.currentTimeMillis() / 1000)
      .withIncrementality(Incrementality.FULL_DATASET)

  }

  private def feedEntitiesUpdates(message: FeedMessage, vrObjects: List[VRObject]): Seq[FeedEntity] = {
    vrObjects.map(feedEntity)
  }

  private def feedEntitiesPositions(message: FeedMessage, trainTracking: List[TrainTracking]): Seq[FeedEntity] = {
    trainTracking.map(feedEntity)
  }

  private def feedEntity(vrObject: VRObject): FeedEntity = {
    val al = alert(vrObject)
    FeedEntity.defaultInstance
      .withId(vrObject.version.toString)
      .withTripUpdate(tripUpdate(vrObject))
    //al.map(tripUpd.withAlert).getOrElse(tripUpd)
  }

  private def feedEntity(trainTracking: TrainTracking): FeedEntity = {
    FeedEntity.defaultInstance
      .withId(trainTracking.version.toString)
      .withVehicle(vehicle(trainTracking))
  }

  private def tripUpdate(obj: VRObject): TripUpdate = {
    TripUpdate.defaultInstance
      .withTrip(tripDescriptor(obj))
      .withStopTimeUpdate(stopTimeUpdate(obj))
  }

  private def vehicle(obj: TrainTracking): VehiclePosition = {
    VehiclePosition.defaultInstance
        .withStopId(obj.trackSection) // We only have the track section
        .withTimestamp(System.currentTimeMillis() / 1000)
        .withVehicle(vehicleDescriptor(obj))
        .withTrip(vpTripDescriptor(obj))
  }

  private def vpTripDescriptor(obj: TrainTracking): TripDescriptor = {
    compareDateTime(obj.timestamp)
    val fetcher = new URLFetcher(Option(Credentials("","")))
    val in = fetcher.urlConnection(properties.getProperty("train.follow").format(obj.trainNumber.replaceAll("[^0-9]","")))
    val s = Source.fromInputStream(in).mkString
    in.close()
    val json = JsonMethods.parse(s)
    val entity = passengerTrainsFromJson(json).entity
    entity match {
      case entity: Seq[FeedEntity] if !entity.isEmpty => TripDescriptor.defaultInstance
        .withTripId(entity.head.getTripUpdate.trip.getTripId)
      case _ => TripDescriptor.defaultInstance

    }
  }

  private def tripDescriptor(obj: VRObject): TripDescriptor = {
    val weekDay = getWeekDay(obj.timeTableRows.head.scheduledTime)
    TripDescriptor.defaultInstance
      .withRouteId(if (obj.commuterLineID.getOrElse("") == "")
        s"${obj.operatorShortCode}_${obj.trainType}${obj.trainNumber}"
      else
        s"${obj.trainType}-${obj.commuterLineID.get}")
      .withTripId(s"${obj.commuterLineID.head}_${weekDay}_${staticDataDateFormat(obj.timeTableRows.head.scheduledTime)}_${staticDataTimeFormat(obj.timeTableRows.head.scheduledTime)}")
      .withScheduleRelationship(
        if (obj.timetableType == VROBJECT_TYPE_REGULAR)
          ScheduleRelationship.SCHEDULED
        else
          ScheduleRelationship.ADDED
      )
  }

  private def vehicleDescriptor(obj: TrainTracking): VehicleDescriptor = {
    // We don't have engine numbers or other ways of telling apart the vehicles
    VehicleDescriptor.defaultInstance
      .withId(obj.trainNumber.toString)
  }

  private def stopTimeUpdate(obj: VRObject): Seq[StopTimeUpdate] = {
    val (arrivals, departures) = obj.timeTableRows.filter(t => t.trainStopping || t.cancelled)
      .partition(_.`type` == VROBJECT_STOPTYPE_ARRIVAL)
    val sortedArrivals = arrivals.sortBy(a => toUnixTime(a.scheduledTime))
    val sortedDepartures = departures.sortBy(d => toUnixTime(d.scheduledTime))
    Seq(departureStopTimeUpdate(sortedDepartures.head)) ++
    sortedArrivals.zip(sortedDepartures.tail).zipWithIndex.map(x => stopTimeUpdate(x)) ++
    Seq(arrivalStopTimeUpdate(sortedArrivals.last, sortedArrivals.size + 1 ))
  }

  private def departureStopTimeUpdate(departure: TimeTableRow) = {
    val scheduledTimeDep = toUnixTime(departure.scheduledTime)
    val estimatedTimeDep = departure.liveEstimateTime.map(toUnixTime)
    StopTimeUpdate.defaultInstance
      // TODO: Map to static data stop ids
      // TODO: uncertainty values
      .withDeparture(stopTimeEvent(estimatedTimeDep.map(est => (est - scheduledTimeDep).toInt), estimatedTimeDep, None))
      .withScheduleRelationship(scheduleRelationship(true, departure.cancelled))
      .withStopSequence(1)
  }

  private def arrivalStopTimeUpdate(arrival: TimeTableRow, index: Int) = {
    val scheduledTimeArr = toUnixTime(arrival.scheduledTime)
    val estimatedTimeArr = arrival.liveEstimateTime.map(toUnixTime)
    StopTimeUpdate.defaultInstance
      // TODO: Map to static data stop ids
      // TODO: uncertainty values
      .withArrival(stopTimeEvent(estimatedTimeArr.map(est => (est - scheduledTimeArr).toInt), estimatedTimeArr, None))
      .withScheduleRelationship(scheduleRelationship(arrival.trainStopping, arrival.cancelled))
      .withStopSequence(index)
  }

  private def stopTimeUpdate(obj: ((TimeTableRow, TimeTableRow), Int)): StopTimeUpdate = {
    val ((arrival, departure), index) = obj
    if (arrival.stationShortCode != departure.stationShortCode)
      System.err.println(s"Non-consecutive station list, arrival is ${arrival.stationShortCode}, departure is ${departure.stationShortCode}")
    val scheduledTimeArr = toUnixTime(arrival.scheduledTime)
    val estimatedTimeArr = arrival.liveEstimateTime.map(toUnixTime)
    val scheduledTimeDep = toUnixTime(departure.scheduledTime)
    val estimatedTimeDep = departure.liveEstimateTime.map(toUnixTime)
    StopTimeUpdate.defaultInstance
      // TODO: Map to static data stop ids
      // TODO: uncertainty values
      .withArrival(stopTimeEvent(estimatedTimeArr.map(est => (est - scheduledTimeArr).toInt), estimatedTimeArr, None))
      .withDeparture(stopTimeEvent(estimatedTimeDep.map(est => (est - scheduledTimeDep).toInt), estimatedTimeDep, None))
      .withScheduleRelationship(scheduleRelationship(arrival.trainStopping, arrival.cancelled))
      .withStopSequence(index + 2) // We handle departure separately
  }

  private def stopTimeEvent(delay: Option[Int], time: Option[Long], uncertainty: Option[Int]): StopTimeEvent = {
    (time,delay,uncertainty) match {
      case (Some(t), Some(d), Some(u)) => StopTimeEvent.defaultInstance.withTime(t).withUncertainty(u)
      case (None, Some(d), Some(u)) => StopTimeEvent.defaultInstance
      case (Some(t), None, Some(u)) => StopTimeEvent.defaultInstance.withTime(t).withUncertainty(u)
      case (Some(t), Some(d), None) => StopTimeEvent.defaultInstance.withTime(t)
      case (None, None, Some(u)) => StopTimeEvent.defaultInstance
      case (Some(t), None, None) => StopTimeEvent.defaultInstance.withTime(t)
      case (None, Some(d), None) => StopTimeEvent.defaultInstance
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

  def compareDateTime(feedTimestamp:String): Boolean = {
    val tz = TimeZone.getTimeZone("UTC")
    val df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
    df.setTimeZone(tz)
    val feedHourMin = staticDataTimeFormat(feedTimestamp)
    val currHourMin = staticDataTimeFormat(df.format(new Date()))
    feedHourMin == currHourMin
  }

  def extractDate(isoDateTime: String):String = {
    val sdf = new SimpleDateFormat("yyyyMMdd")
    sdf.setTimeZone(new SimpleTimeZone(SimpleTimeZone.UTC_TIME, "UTC"))
    sdf.format(ISODateTimeFormat.dateTimeParser().parseDateTime(isoDateTime).toDate)
  }
  def extractTime(isoDateTime: String):String = {
    val sdf = new SimpleDateFormat("HH:mm:ss")
    sdf.setTimeZone(new SimpleTimeZone(SimpleTimeZone.UTC_TIME, "UTC"))
    sdf.format(ISODateTimeFormat.dateTimeParser().parseDateTime(isoDateTime).toDate)
  }

  def staticDataDateFormat(isoDateTime:String): String = {
    val sdf = new SimpleDateFormat("yyyy-MM-dd")
    sdf.setTimeZone(new SimpleTimeZone(SimpleTimeZone.UTC_TIME, "UTC"))
    sdf.format(ISODateTimeFormat.dateTimeParser().parseDateTime(isoDateTime).toDate)
  }

  def staticDataTimeFormat(isoDateTime:String): String = {
    val sdf = new SimpleDateFormat("HHmm")
    sdf.setTimeZone(new SimpleTimeZone(SimpleTimeZone.UTC_TIME, "UTC"))
    sdf.format(ISODateTimeFormat.dateTimeParser().parseDateTime(isoDateTime).toDate)
  }

  def getWeekDay(isoDateTime: String):Int = {
    val c = Calendar.getInstance()
    c.setTime(ISODateTimeFormat.dateTimeParser().parseDateTime(isoDateTime).toDate)
    c.get(Calendar.DAY_OF_WEEK)
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

