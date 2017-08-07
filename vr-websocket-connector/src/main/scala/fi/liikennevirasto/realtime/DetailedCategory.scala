package fi.liikennevirasto.realtime

import com.google.transit.realtime.gtfsrt.Alert.Cause
import fi.liikennevirasto.realtime.Category._

sealed trait DetailedCategory {
  def id: Int
  def detailedCategoryCode: String
  def detailedCategoryName: String
  def categoryGroup: Category
  def overrideCause: Option[Cause] = None // Used to override cause category for categoryGroup above
}
object DetailedCategory {
  val values: Set[DetailedCategory] = Set[DetailedCategory](StuckOnSlope, RecurrentDevianceFromSchedule,
    TrainCancelled, DepartedAheadOfTime, FasterThanScheduled, LackOfPersonnel, ReadinessOrAnomalyReportNotReceived,
    OtherPersonnel, ExceptionalWeatherCondition, SlipperyTrack, VandalismOrTrespassing, Unspecified, TrainCarConnecting,
    WaitingForCars, Breakdown, EquipmentConditionLimitedSpeed, Coupling, CouplingRelease, UnInspectedEquipment,
    WaitingForConnectingRoute, BypassingOrFollowingTrain, CapacityConflict, ForeignOperatingDelay, TrainAhead,
    ArrivingRouteLateOrCancelled, OperatingFault, StopTimeBeyondScheduled, ExtraStop, RanOverPerson, RanOverAnimal,
    LevelCrossingAccident, OtherAccidentOrDamage, TrackEquipmentFailure, DataSystemsFailure, MonitoringEquipmentFailure,
    CommsFailure, PlannedTrackMaintenance, ExceededTimeOnTrackMaintenance, LimitationsAfterTrackMaintenance,
    ElectricityDistributionDisruption, ElectrifiedTrackFailure, LongTermSpeedLimit, TrafficControlSetSpeedLimit,
    TrackFaultOrBlock, WaitingForEngine, EngineFailure, EnginePowerReducedSpeed, UnInspectedEngine, Unknown)

  def apply(code: String): DetailedCategory = {
    values.find(_.detailedCategoryCode == code).getOrElse(Unknown)
  }
  case object StuckOnSlope extends DetailedCategory {
    def id = 95
    def detailedCategoryCode = "A1"
    def detailedCategoryName = "Mäkeenjäänti"
    def categoryGroup = SchedulingAndOperating  }
  case object RecurrentDevianceFromSchedule extends DetailedCategory {
    def id = 96
    def detailedCategoryCode = "A2"
    def detailedCategoryName = "Toistuva poikkeama aikataulusta"
    def categoryGroup = SchedulingAndOperating  }
  case object TrainCancelled extends DetailedCategory {
    def id = 97
    def detailedCategoryCode = "A3"
    def detailedCategoryName = "Junaa ei ole ajettu"
    def categoryGroup = SchedulingAndOperating  }
  case object DepartedAheadOfTime extends DetailedCategory {
    def id = 98
    def detailedCategoryCode = "E1"
    def detailedCategoryName = "Etuajassa lähtö"
    def categoryGroup = AheadOfTime   }
  case object FasterThanScheduled extends DetailedCategory {
    def id = 99
    def detailedCategoryCode = "E2"
    def detailedCategoryName = "Ajo- tai pysähdysajan alitus"
    def categoryGroup = AheadOfTime   }
  case object LackOfPersonnel extends DetailedCategory {
    def id = 81
    def detailedCategoryCode = "H1"
    def detailedCategoryName = "Henkilökunta puuttuu"
    def categoryGroup = Personnel   }
  case object ReadinessOrAnomalyReportNotReceived extends DetailedCategory {
    def id = 82
    def detailedCategoryCode = "H2"
    def detailedCategoryName = "Lähtövalmius- tai lähtöpoikkeamailmoitus tekemättä"
    def categoryGroup = Personnel   }
  case object OtherPersonnel extends DetailedCategory {
    def id = 83
    def detailedCategoryCode = "H3"
    def detailedCategoryName = "Muu henkilökuntasyy"
    def categoryGroup = Personnel   }
  case object ExceptionalWeatherCondition extends DetailedCategory {
    def id = 125
    def detailedCategoryCode = "I1"
    def detailedCategoryName = "Poikkeukselliset sääolosuhteet"
    def categoryGroup = Other
    override def overrideCause = Some(Cause.WEATHER)}
  case object SlipperyTrack extends DetailedCategory {
    def id = 126
    def detailedCategoryCode = "I2"
    def detailedCategoryName = "Lehtikeli tai muu liukkaus"
    def categoryGroup = Other
    override def overrideCause = Some(Cause.WEATHER)}
  case object VandalismOrTrespassing extends DetailedCategory {
    def id = 127
    def detailedCategoryCode = "I3"
    def detailedCategoryName = "Ilkivalta, asiaton radalla liikkuminen"
    def categoryGroup = Other   }
  case object Unspecified extends DetailedCategory {
    def id = 128
    def detailedCategoryCode = "I4"
    def detailedCategoryName = "Muu syy"
    def categoryGroup = Other   }
  case object TrainCarConnecting extends DetailedCategory {
    def id = 84
    def detailedCategoryCode = "J1"
    def detailedCategoryName = "Junan muodostaminen ratapihalla tai lähtöraiteella"
    def categoryGroup = CarConnecting   }
  case object WaitingForCars extends DetailedCategory {
    def id = 85
    def detailedCategoryCode = "K1"
    def detailedCategoryName = "Kaluston odotus"
    def categoryGroup = TechEnginesAndCars   }
  case object Breakdown extends DetailedCategory {
    def id = 86
    def detailedCategoryCode = "K2"
    def detailedCategoryName = "Kalustovika"
    def categoryGroup = TechEnginesAndCars   }
  case object EquipmentConditionLimitedSpeed extends DetailedCategory {
    def id = 87
    def detailedCategoryCode = "K3"
    def detailedCategoryName = "Kalustosta johtuva nopeuden alennus"
    def categoryGroup = TechEnginesAndCars   }
  case object Coupling extends DetailedCategory {
    def id = 88
    def detailedCategoryCode = "K4"
    def detailedCategoryName = "Kytkentä"
    def categoryGroup = TechEnginesAndCars   }
  case object CouplingRelease extends DetailedCategory {
    def id = 89
    def detailedCategoryCode = "K5"
    def detailedCategoryName = "Irroitus"
    def categoryGroup = TechEnginesAndCars   }
  case object UnInspectedEquipment extends DetailedCategory {
    def id = 90
    def detailedCategoryCode = "K6"
    def detailedCategoryName = "Katsastamaton kalusto"
    def categoryGroup = TechEnginesAndCars   }
  case object WaitingForConnectingRoute extends DetailedCategory {
    def id = 100
    def detailedCategoryCode = "L1"
    def detailedCategoryName = "Yhteysliikenteen odotus (jatkoyhteydet)"
    def categoryGroup = TrafficControl   }
  case object BypassingOrFollowingTrain extends DetailedCategory {
    def id = 101
    def detailedCategoryCode = "L2"
    def detailedCategoryName = "Junakohtaus, edellä kulkeva juna tai ohitus"
    def categoryGroup = TrafficControl   }
  case object CapacityConflict extends DetailedCategory {
    def id = 102
    def detailedCategoryCode = "L3"
    def detailedCategoryName = "Konflikti kapasiteetissa"
    def categoryGroup = TrafficControl   }
  case object ForeignOperatingDelay extends DetailedCategory {
    def id = 103
    def detailedCategoryCode = "L4"
    def detailedCategoryName = "Myöhästyminen ulkomailta"
    def categoryGroup = TrafficControl   }
  case object TrainAhead extends DetailedCategory {
    def id = 104
    def detailedCategoryCode = "L5"
    def detailedCategoryName = "Edessä oleva kalusto tukkii radan"
    def categoryGroup = TrafficControl   }
  case object ArrivingRouteLateOrCancelled extends DetailedCategory {
    def id = 105
    def detailedCategoryCode = "L6"
    def detailedCategoryName = "Tulojuna myöhässä / peruttu"
    def categoryGroup = TrafficControl   }
  case object OperatingFault extends DetailedCategory {
    def id = 106
    def detailedCategoryCode = "L7"
    def detailedCategoryName = "Liikenteenhoitovirhe"
    def categoryGroup = TrafficControl   }
  case object StopTimeBeyondScheduled extends DetailedCategory {
    def id = 123
    def detailedCategoryCode = "M1"
    def detailedCategoryName = "Pysähtymisajan ylitys"
    def categoryGroup = PassengerServices   }
  case object ExtraStop extends DetailedCategory {
    def id = 124
    def detailedCategoryCode = "M2"
    def detailedCategoryName = "Ylimääräinen pysähdys"
    def categoryGroup = PassengerServices   }
  case object RanOverPerson extends DetailedCategory {
    def id = 119
    def detailedCategoryCode = "O1"
    def detailedCategoryName = "Allejäänti (ihminen)"
    def categoryGroup = Accident   }
  case object RanOverAnimal extends DetailedCategory {
    def id = 120
    def detailedCategoryCode = "O2"
    def detailedCategoryName = "Allejäänti (eläin)"
    def categoryGroup = Accident   }
  case object LevelCrossingAccident extends DetailedCategory {
    def id = 121
    def detailedCategoryCode = "O3"
    def detailedCategoryName = "Tasoristeysonnettomuus"
    def categoryGroup = Accident   }
  case object OtherAccidentOrDamage extends DetailedCategory {
    def id = 122
    def detailedCategoryCode = "O4"
    def detailedCategoryName = "Muut onnettomuudet ja vauriot"
    def categoryGroup = Accident   }
  case object TrackEquipmentFailure extends DetailedCategory {
    def id = 107
    def detailedCategoryCode = "P1"
    def detailedCategoryName = "Ratainfran laiteviat"
    def categoryGroup = TrafficRoutingSystems   }
  case object DataSystemsFailure extends DetailedCategory {
    def id = 108
    def detailedCategoryCode = "P2"
    def detailedCategoryName = "Tietojärjestelmäviat"
    def categoryGroup = TrafficRoutingSystems   }
  case object MonitoringEquipmentFailure extends DetailedCategory {
    def id = 109
    def detailedCategoryCode = "P3"
    def detailedCategoryName = "Valvontalaitevika"
    def categoryGroup = TrafficRoutingSystems   }
  case object CommsFailure extends DetailedCategory {
    def id = 110
    def detailedCategoryCode = "P4"
    def detailedCategoryName = "Viestintälaite /-yhteys viat"
    def categoryGroup = TrafficRoutingSystems   }
  case object PlannedTrackMaintenance extends DetailedCategory {
    def id = 116
    def detailedCategoryCode = "R1"
    def detailedCategoryName = "Ratatyöt (ennakoidut työt nopeusrajoituksineen)"
    def categoryGroup = TrackMaintenance   }
  case object ExceededTimeOnTrackMaintenance extends DetailedCategory {
    def id = 117
    def detailedCategoryCode = "R2"
    def detailedCategoryName = "Ratatyön sovitun ajan ylitys"
    def categoryGroup = TrackMaintenance   }
  case object LimitationsAfterTrackMaintenance extends DetailedCategory {
    def id = 118
    def detailedCategoryCode = "R3"
    def detailedCategoryName = "Liikennerajoite ratatyön jälkeen"
    def categoryGroup = TrackMaintenance   }
  case object ElectricityDistributionDisruption extends DetailedCategory {
    def id = 111
    def detailedCategoryCode = "S1"
    def detailedCategoryName = "Sähkönjakeluhäiriö"
    def categoryGroup = ElectricTrack   }
  case object ElectrifiedTrackFailure extends DetailedCategory {
    def id = 112
    def detailedCategoryCode = "S2"
    def detailedCategoryName = "Sähköratavika"
    def categoryGroup = ElectricTrack   }
  case object LongTermSpeedLimit extends DetailedCategory {
    def id = 113
    def detailedCategoryCode = "T1"
    def detailedCategoryName = "Pitkäaikaiset nopeusrajoitukset"
    def categoryGroup = Track   }
  case object TrafficControlSetSpeedLimit extends DetailedCategory {
    def id = 114
    def detailedCategoryCode = "T2"
    def detailedCategoryName = "Liikenteenohjauksen asettamat nopeusrajoitukset (ratarakenne)"
    def categoryGroup = Track   }
  case object TrackFaultOrBlock extends DetailedCategory {
    def id = 115
    def detailedCategoryCode = "T3"
    def detailedCategoryName = "Ratarikko / este radalla"
    def categoryGroup = Track   }
  case object WaitingForEngine extends DetailedCategory {
    def id = 91
    def detailedCategoryCode = "V1"
    def detailedCategoryName = "Veturin odotus junaan"
    def categoryGroup = Engine   }
  case object EngineFailure extends DetailedCategory {
    def id = 92
    def detailedCategoryCode = "V2"
    def detailedCategoryName = "Veturivika"
    def categoryGroup = Engine   }
  case object EnginePowerReducedSpeed extends DetailedCategory {
    def id = 93
    def detailedCategoryCode = "V3"
    def detailedCategoryName = "Vetovoimasta johtuva nopeuden alennus / tehon puute"
    def categoryGroup = Engine   }
  case object UnInspectedEngine extends DetailedCategory {
    def id = 94
    def detailedCategoryCode = "V4"
    def detailedCategoryName = "Katsastamaton vetokalusto"
    def categoryGroup = Engine   }

  case object Unknown extends DetailedCategory {
    def id = 0
    def detailedCategoryCode = ""
    def detailedCategoryName = "N/A"
    def categoryGroup = Category.Unknown }
}
