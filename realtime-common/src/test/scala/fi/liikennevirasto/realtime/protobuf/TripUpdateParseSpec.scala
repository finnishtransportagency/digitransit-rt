package fi.liikennevirasto.realtime.protobuf

import com.google.transit.realtime.gtfsrt.{FeedEntity, FeedMessage, TripUpdate}
import org.scalatest.{FunSuite, Matchers}

class TripUpdateParseSpec extends FunSuite with Matchers{
  test("Trip Updates should be parseable as a message") {
    val inputStream = this.getClass.getResourceAsStream("/trip-updates.bin")
    inputStream should not be (null)
    val feed = FeedMessage.parseFrom(inputStream)
    feed should not be (null)
    feed.entity.forall(_.vehicle.isEmpty) should be (true)
    feed.entity.forall(_.alert.isEmpty) should be (true)
    feed.entity.forall(_.tripUpdate.isDefined) should be (true)
  }

  test("Lahti Trip Updates should contain a time") {
    val inputStream = this.getClass.getResourceAsStream("/lahti-tripupdates.bin")
    inputStream should not be (null)
    val feed = FeedMessage.parseFrom(inputStream)
    feed should not be (null)
    feed.entity.forall(_.vehicle.isEmpty) should be (true)
    feed.entity.forall(_.alert.isEmpty) should be (true)
    feed.entity.forall(_.tripUpdate.isDefined) should be (true)
    feed.entity.exists(_.tripUpdate.exists(_.stopTimeUpdate.exists(_.arrival.exists(_.time.isDefined)))) should be (true)
  }

}
