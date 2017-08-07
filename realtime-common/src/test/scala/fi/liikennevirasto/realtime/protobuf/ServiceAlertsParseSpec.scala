package fi.liikennevirasto.realtime.protobuf

import com.google.transit.realtime.gtfsrt.{Alert, FeedMessage}
import org.scalatest.{FunSuite, Matchers}

class ServiceAlertsParseSpec extends FunSuite with Matchers{
  test("Alerts should be parseable as a message") {
    val inputStream = this.getClass.getResourceAsStream("/service-alerts.bin")
    inputStream should not be (null)
    val feed = FeedMessage.parseFrom(inputStream)
    feed should not be (null)
    feed.entity.forall(_.vehicle.isEmpty) should be (true)
    feed.entity.forall(_.alert.isDefined) should be (true)
    feed.entity.forall(_.tripUpdate.isEmpty) should be (true)
  }
}
