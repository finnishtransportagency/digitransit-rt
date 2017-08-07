package fi.liikennevirasto.realtime

import com.google.transit.realtime.gtfsrt.Alert.Cause
import com.google.transit.realtime.gtfsrt.Alert.Cause.{ACCIDENT, MAINTENANCE, OTHER_CAUSE, TECHNICAL_PROBLEM, UNKNOWN_CAUSE}

sealed trait Category {
  def id: Int
  def categoryCode: String
  def categoryName: String
  def gtfsCause: Cause
}
object Category {
  val values: Set[Category] = Set[Category](Personnel, CarConnecting, TechEnginesAndCars, Engine, SchedulingAndOperating,
    AheadOfTime, TrafficControl, TrafficRoutingSystems, ElectricTrack, Track, TrackMaintenance, Accident,
    PassengerServices, Other, Unknown)

  def apply(code: String): Category = {
    values.find(_.categoryCode == code).getOrElse(Unknown)
  }

  case object Personnel extends Category {
    def id = 21
    def categoryCode = "H"
    def categoryName = "Henkilökunta"
    def gtfsCause = OTHER_CAUSE
  }
  case object CarConnecting extends Category {
    def id = 22
    def categoryCode = "J"
    def categoryName = "Junanmuodostus"
    def gtfsCause = TECHNICAL_PROBLEM }
  case object TechEnginesAndCars extends Category {
    def id = 23
    def categoryCode = "K"
    def categoryName = "Kalusto, moottorijunat ja vaunut"
    def gtfsCause = TECHNICAL_PROBLEM }
  case object Engine extends Category {
    def id = 24
    def categoryCode = "V"
    def categoryName = "Vetokalusto"
    def gtfsCause = TECHNICAL_PROBLEM }
  case object SchedulingAndOperating extends Category {
    def id = 25
    def categoryCode = "A"
    def categoryName = "Aikataulu ja liikennöinti"
    def gtfsCause = OTHER_CAUSE }
  case object AheadOfTime extends Category {
    def id = 26
    def categoryCode = "E"
    def categoryName = "Etuajassakulku"
    def gtfsCause = OTHER_CAUSE }
  case object TrafficControl extends Category {
    def id = 27
    def categoryCode = "L"
    def categoryName = "Liikenteenhoito"
    def gtfsCause = OTHER_CAUSE}
  case object TrafficRoutingSystems extends Category {
    def id = 28
    def categoryCode = "P"
    def categoryName = "Liikenteenhoitojärjestelmät"
    def gtfsCause = TECHNICAL_PROBLEM }
  case object ElectricTrack extends Category {
    def id = 29
    def categoryCode = "S"
    def categoryName = "Sähkörata"
    def gtfsCause = TECHNICAL_PROBLEM }
  case object Track extends Category {
    def id = 30
    def categoryCode = "T"
    def categoryName = "Rata (ratarakenne)"
    def gtfsCause = TECHNICAL_PROBLEM }
  case object TrackMaintenance extends Category {
    def id = 31
    def categoryCode = "R"
    def categoryName = "Ratatyö"
    def gtfsCause = MAINTENANCE }
  case object Accident extends Category {
    def id = 32
    def categoryCode = "O"
    def categoryName = "Onnettomuus"
    def gtfsCause = ACCIDENT }
  case object PassengerServices extends Category {
    def id = 33
    def categoryCode = "M"
    def categoryName = "Matkustajapalvelu"
    def gtfsCause = OTHER_CAUSE }
  case object Other extends Category {
    def id = 34
    def categoryCode = "I"
    def categoryName = "Muut syyt"
    def gtfsCause = OTHER_CAUSE }
  case object Unknown extends Category {
    def id = 0
    def categoryCode = ""
    def categoryName = "N/A"
    def gtfsCause = UNKNOWN_CAUSE }
}
