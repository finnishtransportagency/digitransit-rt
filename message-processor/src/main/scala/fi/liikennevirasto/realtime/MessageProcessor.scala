package fi.liikennevirasto.realtime

import java.nio.charset.Charset
import java.util.Properties

import com.google.transit.realtime.gtfsrt.FeedMessage
import fi.liikennevirasto.realtime.dao.DataBase
import fi.liikennevirasto.realtime.strategy._
import org.json4s.{DefaultFormats, NoTypeHints}
import org.json4s.jackson.JsonMethods.parse
import org.json4s.jackson.Serialization
import org.slf4j.LoggerFactory

/**
  * A class to initialize the application, read the environment properties and set up listeners etc
  */
object MessageProcessor extends App {

  private val log = LoggerFactory.getLogger(this.getClass)
  implicit val formats = Serialization.formats(NoTypeHints) + new LongSerializer()

  lazy val properties: Properties = {
    val props = new Properties()
    props.load(getClass.getResourceAsStream("/realtime.properties"))
    log.info("Properties loaded from /realtime.properties")
    props
  }

  val brokerUrl = this.properties.getProperty("broker.host")
  val subscribePort = this.properties.getProperty("broker.port")
  val connectors = this.properties.getProperty("connectors", "").split(',')
  val connectorConfigs = connectors.map(mkConfig(this.properties))

  if (brokerUrl != "" && subscribePort != "" && connectors.length != 0) {
    postgresConnect()

    connectorConfigs.foreach(config => {
      new MQTTListener(brokerUrl, subscribePort, config.vehicleInputTopic, config.vehicleOutputTopic,
        s"${config.connector}-vehicles", forwardStrategy(config.vehicleStrategy, config.connector, config.vehicleOutputTopic))
      log.info(s"${config.connector}-vehicles")
      new MQTTListener(brokerUrl, subscribePort, config.tripUpdateInputTopic, config.tripUpdateOutputTopic,
        s"${config.connector}-tripupdates", forwardStrategy(config.tripUpdateStrategy, config.connector, config.tripUpdateOutputTopic))
      log.info(s"${config.connector}-tripupdates")
      new MQTTListener(brokerUrl, subscribePort, config.serviceAlertInputTopic, config.serviceAlertOutputTopic,
        s"${config.connector}-servicealert", forwardStrategy(config.serviceAlertStrategy, config.connector, config.serviceAlertOutputTopic))
      log.info(s"${config.connector}-servicealert")
    })
  } else {
    log.info("Missing broker, port or connectors (%s, %s, %s).".format(brokerUrl, subscribePort, connectors.mkString(",")))
    log.info("Need at least these parameters for starting")
    System.exit(1)
  }

  private def mkConfig(properties: Properties)(connector: String) = {
    ConnectorConfig(connector,
      properties.getProperty("connector.%s.vehicles.strategy".format(connector)),
      properties.getProperty("connector.%s.vehicles.input".format(connector)),
      properties.getProperty("connector.%s.vehicles.output".format(connector)),
      properties.getProperty("connector.%s.tripupdates.strategy".format(connector)),
      properties.getProperty("connector.%s.tripupdates.input".format(connector)),
      properties.getProperty("connector.%s.tripupdates.output".format(connector)),
      properties.getProperty("connector.%s.servicealert.strategy".format(connector)),
      properties.getProperty("connector.%s.servicealert.input".format(connector)),
      properties.getProperty("connector.%s.servicealert.output".format(connector))
    )
  }

  def postgresConnect(): Unit = {
    try {
      DataBase.db.createSession()
      log.info("Connection to Postgres established")
    } catch {
      case ex: Exception => log.error("Error connecting to Postgres: ", ex)
        log.error(s"URL was ${DataBase.getConnectionURL}")
        System.exit(1)
    } finally {
      DataBase.db.close()
    }
  }

  def shutdown(): Unit = {
    log.info(s"<$mqttSubscriber> stopped")
  }

  def forwardStrategy(strategy: String, connectorName: String, topic: String): Array[Byte] => Seq[RealTimeMessage] = {
    def forward(payload: Array[Byte]) = {
      Seq(RealTimeMessage(connectorName, payload, topic))
    }

    def keepProtobuf(payload: Array[Byte]) = {
      val msg = parse(new String(payload, Charset.defaultCharset())).extract[FeedMessage]
      Seq(RealTimeMessage(connectorName,msg.toByteArray,topic))
    }

    def convertStopId(payload: Array[Byte]) = {
      Seq(RealTimeMessage(connectorName, ConvertStopIds.handleMessage(connectorName, payload), topic))
    }

    def convertTripId(payload: Array[Byte]) = {
      Seq(RealTimeMessage(connectorName, ConvertTripIds.handleMessage(connectorName, payload), topic))
    }

    def convertTrainId(payload: Array[Byte]) = {
      Seq(RealTimeMessage(connectorName, ConvertVrTripIds.handleMessage(connectorName, payload), topic))
    }

    def vehiclePosition(payload: Array[Byte]) = {
      val msg = parse(new String(payload, Charset.defaultCharset())).extract[FeedMessage]
      if (msg.entity.isEmpty) {
        log.info(s"Received empty message from $connectorName")
        keepProtobuf(payload)
      } else {
        val msgSeq = connectorName match {
          case "oulu" => {
            msg.entity.map(entity => {
              OuluVehiclePosition.handleMessage(connectorName, entity.vehicle.head)
            }).tail
          }
          case "lahti" => {
            msg.entity.map(entity => {
              LahtiVehiclePosition.handleMessage(connectorName, entity.vehicle.head)
            }).tail
          }
          case "jyvaskyla" => {
            msg.entity.map(entity => {
              JyvaskylaVehiclePosition.handleMessage(connectorName, entity.vehicle.head)
            }).tail
          }
          case "vr" => {
              msg.entity.map(entity => {
                VrVehiclePosition.handleMessage(connectorName, entity.vehicle.head)
              }).tail
          }
          case _ => forward(payload)
        }
        msgSeq
      }
    }

    strategy match {
      case "forward" => forward
      case "keep-protobuf" => keepProtobuf
      case "convert-stop-id" => convertStopId
      case "convert-trip-id" => convertTripId
      case "vehicle-position" => vehiclePosition
      case "convert-train-trip-id" => convertTrainId
      case _ =>
        log.error("Invalid strategy %s specified, using forward".format(strategy))
        forward
    }
  }

}

case class ConnectorConfig(connector: String, vehicleStrategy: String, vehicleInputTopic: String,
                           vehicleOutputTopic: String, tripUpdateStrategy: String, tripUpdateInputTopic: String,
                           tripUpdateOutputTopic: String, serviceAlertStrategy: String, serviceAlertInputTopic: String,
                           serviceAlertOutputTopic: String)

case class RealTimeMessage(connector: String, message: Array[Byte], topic: String)