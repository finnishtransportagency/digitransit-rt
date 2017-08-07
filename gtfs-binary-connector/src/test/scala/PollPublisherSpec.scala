import akka.actor.Props
import com.google.transit.realtime.gtfsrt.FeedMessage
import com.trueaccord.scalapb.json.JsonFormat
import fi.liikennevirasto.realtime.{BinaryURLPoller, Publisher, properties}
import org.scalatest.mock.MockitoSugar
import org.scalatest.{FunSuite, Matchers}

class PollPublisherSpec extends FunSuite with Matchers with MockitoSugar {
  val system = akka.actor.ActorSystem("system")

  var finished = false
  var recMessage: String = ""
  class PublisherTest(val host: String, val port: String, val from: String, val sendTo: String) extends
    Publisher {
    override def publish(message: String): Unit = {
      finished = true
      recMessage = message
    }
  }
  test("Oulu feed is read and published") {
    val WAIT_MAX = 1000
    val publisher = new PublisherTest("localhost", "1883", "foo", "bar")
    val pollPublisher = system.actorOf(Props(new BinaryURLPoller(publisher)))
    system.eventStream.subscribe(pollPublisher, classOf[String])
    system.eventStream.publish("http://92.62.36.215/RTIX/trip-updates")
    var waitCounter = 0
    while(!finished && waitCounter < WAIT_MAX) {
      Thread.sleep(10)
      waitCounter = waitCounter + 1
    }
    if (waitCounter >= WAIT_MAX)
      throw new RuntimeException("Wait maximum reached!")
    val feedMessage = JsonFormat.fromJsonString[FeedMessage](recMessage)
    feedMessage shouldNot be (null)
    feedMessage.header.timestamp.nonEmpty should be (true)
  }

  test("Lahti feed is read and published") {
    val WAIT_MAX = 1000
    val publisher = new PublisherTest("localhost", "1883", "foo", "bar")
    val pollPublisher = system.actorOf(Props(new BinaryURLPoller(publisher)))
    properties.setProperty("source.user", "cgipilotti")
    properties.setProperty("source.pass", "lvigcfi49")
    system.eventStream.subscribe(pollPublisher, classOf[String])
    system.eventStream.publish("https://lsl.mattersoft.fi/api/gtfsrealtime/v1.0/feed/tripupdate")
    var waitCounter = 0
    while(!finished && waitCounter < WAIT_MAX) {
      Thread.sleep(10)
      waitCounter = waitCounter + 1
    }
    if (waitCounter >= WAIT_MAX)
      throw new RuntimeException("Wait maximum reached!")
    val feedMessage = JsonFormat.fromJsonString[FeedMessage](recMessage)
    feedMessage shouldNot be (null)
    feedMessage.header.timestamp.nonEmpty should be (true)
  }
}
