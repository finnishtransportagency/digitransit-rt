package fi.liikennevirasto

import java.io.FileInputStream
import java.nio.file.Paths
import java.util.Properties

package object realtime {

  val mqttSubscriber = "Realtime-Subscriber"
  val mqttPublisher = "Realtime-Publisher"

  lazy val properties: Properties = {
    val moduleProps = new Properties()
    moduleProps.load(new FileInputStream(Paths.get(System.getProperty("user.home"), "/module.properties").toFile))
    val props = new Properties()
    props.load(getClass.getResourceAsStream(s"/gtfs.properties"))
    props.putAll(moduleProps)
    props.list(System.out)
    props
  }

  def convertWeekDay(trainType:String , weekDay: String): String = {
    weekDay match {
      case "1" => s"Su"
      case "2" => s"Ma"
      case "3" => s"Ti"
      case "4" => s"Ke"
      case "5" => s"To"
      case "6" => s"Pe/H3"
      case "7" => s"La"
    }
  }

}
