package fi.liikennevirasto.realtime

import org.eclipse.paho.client.mqttv3._
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import org.slf4j.LoggerFactory

class MQTTListener(host: String, port: String, topic: String, sendTo: String, clientName: String,
                   process: Array[Byte] => Seq[RealTimeMessage]) {

  private val log = LoggerFactory.getLogger(this.getClass)
  private val brokerUrl = s"tcp://$host:$port"
  private val clientId = if (clientName == null || clientName == "") MqttClient.generateClientId() else clientName
  private val persistence = new MemoryPersistence
  private val client = new MqttClient(brokerUrl, clientId, persistence)
  private val options = new MqttConnectOptions()

  options.setCleanSession(true)
  options.setUserName(MessageProcessor.properties.getProperty("mqtt.user"))
  options.setPassword(MessageProcessor.properties.getProperty("mqtt.pass").toCharArray)
  client.connect(options)
  client.subscribe(topic,0)

  log.info(s"Starting connection $host:$port for $topic -> $sendTo publishing")

  val callback = new MqttCallback {
    override def messageArrived(topic: String, message: MqttMessage): Unit = {
      val doStrategy = process(message.getPayload): Seq[RealTimeMessage]
      log.info(topic + " -> " + sendTo + " @ " + System.currentTimeMillis())

      doStrategy.foreach(m => {
        message.setPayload(m.message)
        client.getTopic(m.topic).publish(message)
      })
    }

    override def connectionLost(cause: Throwable): Unit = {
      log.error("Connection Lost, trying to reconnect", cause)
      var i = 0
      for (i <- 1 to 1000) {
        try {
          client.connect(options)
          client.subscribe(topic)
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
    }

  }
  client.setCallback(callback)
}