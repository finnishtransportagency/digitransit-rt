import fi.liikennevirasto.realtime.VRJsonToGTFSMapper
import org.json4s.jackson.JsonMethods
import org.scalatest.{FunSuite, Matchers}

import scala.io.Source

class VRPollerSpec extends FunSuite with Matchers{
  // Couldn't try the actor properly, using the "copy mechanism in test" to find the fault
  test("Convert to GTFS-RT from Json") {
    val stream = this.getClass.getResourceAsStream("/hugelist.json")
    val s = Source.fromInputStream(stream).mkString
    val json = JsonMethods.parse(s)
    val feedMessage = VRJsonToGTFSMapper.passengerTrainsFromJson(json)
    feedMessage.entity.size should be (584)
    VRJsonToGTFSMapper.jsonToVRObjects(json).map(_.version).max should be (228239350321L)
  }
}
