package fi.liikennevirasto.realtime.protobuf

import com.google.transit.realtime.gtfsrt.{FeedMessage, VehiclePosition}
import org.scalatest.{FunSuite, Matchers}

class VehiclePositionParseSpec extends FunSuite with Matchers{
  test("Vehicle Positions should be parseable as a binary message") {
    val inputStream = this.getClass.getResourceAsStream("/vehicle-position.bin")
    inputStream should not be (null)
    val feed = FeedMessage.parseFrom(inputStream)
    feed should not be (null)
    feed.entity.forall(_.vehicle.isDefined) should be (true)
    feed.entity.forall(_.alert.isEmpty) should be (true)
    feed.entity.forall(_.tripUpdate.isEmpty) should be (true)
  }

}
