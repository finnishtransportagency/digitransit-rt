import fi.liikennevirasto.realtime.VRJsonToGTFSMapper
import org.json4s.{DefaultFormats, Formats}
import org.json4s.jackson.JsonMethods
import org.scalatest.{FunSuite, Matchers}

import scala.io.Source

class VRJsonToGTFSMapperSpec extends FunSuite with Matchers{
  test("Convert to GTFS-RT from Json") {
    val jsonString = Source.fromInputStream(this.getClass.getResourceAsStream("/train.json")).mkString("")
    val json = JsonMethods.parse(jsonString)
    // TODO: Validation of received data
  }

  test("Convert to GTFS-RT from Json with cause code") {
    val jsonString = Source.fromInputStream(this.getClass.getResourceAsStream("/withCause.json")).mkString("")
    val json = JsonMethods.parse(jsonString)
    VRJsonToGTFSMapper.passengerTrainsFromJson(json)
    // TODO: Validation of received data
  }

  test("Date extraction from ISO datetime format") {
    VRJsonToGTFSMapper.extractDate("2017-01-20T12:51:57.000Z") should be ("20170120")
    VRJsonToGTFSMapper.extractDate("2017-01-20T00:00:00.000Z") should be ("20170120")
    VRJsonToGTFSMapper.extractDate("2017-01-20T23:59:59.499Z") should be ("20170120")
  }

  test("Time extraction from ISO datetime format") {
    VRJsonToGTFSMapper.extractTime("2017-01-20T12:51:57.000Z") should be ("12:51:57")
    VRJsonToGTFSMapper.extractTime("2017-01-20T00:00:00.000Z") should be ("00:00:00")
    VRJsonToGTFSMapper.extractTime("2017-01-20T23:59:59.499Z") should be ("23:59:59")
  }

  test("Unix timestamp is calculated correctly from GMT") {
    VRJsonToGTFSMapper.toUnixTime("2017-01-20T00:00:00.000Z") % 86400 should be (0)
  }

  test("Unix timestamp is calculated correctly from EET") {
    VRJsonToGTFSMapper.toUnixTime("2017-01-20T03:00:00.000+0300") % 86400 should be (0)
  }
}
