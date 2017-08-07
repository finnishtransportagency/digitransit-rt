package fi.liikennevirasto.realtime.vr

import java.util.Date

import fi.liikennevirasto.realtime.{Category, DetailedCategory}
import org.joda.time.DateTime
import org.json4s.{CustomSerializer, DefaultFormats, Formats}
import org.json4s.JsonAST.JValue


case class VRObject(trainNumber: Int, departureDate: Date, operatorUICCode: Int, operatorShortCode: String,
                    trainType: String, trainCategory: String, commuterLineID: Option[String],
                    runningCurrently: Boolean, cancelled: Boolean, version: Long, timetableType: String,
                    timetableAcceptanceDate: String, timeTableRows: Seq[TimeTableRow]) {
}

case class TimeTableRow(trainStopping: Boolean, stationShortCode: String, stationUICCode: Int,
                        countryCode: String, `type`: String, commercialStop: Option[Boolean],
                        commercialTrack: Option[String], cancelled: Boolean, scheduledTime: String,
                        liveEstimateTime: Option[String], actualTime: Option[String],
                        differenceInMinutes: Option[Int], causes: Seq[CauseCategory])

case class CauseCategory(category: Category, detailedCategory: Option[DetailedCategory])


case object CauseCategorySerializer extends CustomSerializer[CauseCategory](
  format => ( {
    case jValue: JValue =>
      implicit val jsonFormats: Formats = DefaultFormats
      val code = (jValue\\"categoryCode").extract[String]
      val detailCode = (jValue\\"detailedCategoryCode").toOption.map(_.extract[String])
      CauseCategory(Category.apply(code), detailCode.map(DetailedCategory.apply))
  },{
    case c: CauseCategory => throw new NotImplementedError("CauseCategory serialization")
  })
)