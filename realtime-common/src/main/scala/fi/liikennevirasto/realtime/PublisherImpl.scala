package fi.liikennevirasto.realtime

import java.nio.charset.Charset

import org.eclipse.paho.client.mqttv3._
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import org.slf4j.LoggerFactory

/**
  * Message publisher. Handles communication between connectors and Mqtt.
  */
class PublisherImpl(val host: String, val port: String, val from: String, val sendTo: String) extends Publisher {

  private val brokerUrl = s"tcp://$host:$port"
  private val persistence = new MemoryPersistence
  private val client = new MqttClient(brokerUrl, from , persistence)
  private val options = new MqttConnectOptions()
  options.setUserName(properties.getProperty("mqtt.user", "digitransit"))
  options.setPassword(properties.getProperty("mqtt.pass","pwd").toCharArray)
  client.connect(options)

  val log = LoggerFactory.getLogger(this.getClass)

  private val publishTopic = client.getTopic(sendTo)

  log.info(s"Starting connection $host:$port for -> $sendTo publishing")

  val callback = new MqttCallback {
    override def messageArrived(topic: String, message: MqttMessage): Unit = {
    }

    override def connectionLost(cause: Throwable): Unit = {
      log.error("Connection Lost, trying to reconnect", cause)
      var i = 0
      for( i <- 1 to 1000) {
        try {
          client.connect(options)
          return
        } catch {
          case ex: Exception =>
            log.info("Reconnect failed, trying again")
        }
        try {
          Thread.sleep(100)
        } catch {
          case ex: Exception =>
            log.info("Sleep interrupted")
        }
      }
      log.error("Reconnect failed, exiting")
      System.exit(2)
    }

    override def deliveryComplete(token: IMqttDeliveryToken): Unit = {
      log.info(s"Delivery complete ${token.getMessage.getId}")
    }

  }
  client.setCallback(callback)

  override def publish(message: String): Unit = {
    log.info("%s: Publishing message (%d bytes @ %d)".format(sendTo,
      message.length, System.currentTimeMillis()))
    publishTopic.publish(message.getBytes(Charset.defaultCharset()), 0, false)
  }
}
