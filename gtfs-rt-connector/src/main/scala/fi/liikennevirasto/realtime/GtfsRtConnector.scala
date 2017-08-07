package fi.liikennevirasto.realtime

import akka.actor.{Cancellable, Props}
import org.slf4j.LoggerFactory

import scala.concurrent.duration.{FiniteDuration, MILLISECONDS}

/**
  * A class to initialize the application, read the environment properties and set up listeners etc
  */
object GtfsRtConnector extends App with URLConnector {

  private val log = LoggerFactory.getLogger(this.getClass)
  private var cancellable: Cancellable = _

  val system = akka.actor.ActorSystem("system")

  import system.dispatcher

  sys.addShutdownHook {
    shutdown()
  }

  if (!properties.isEmpty) {

    if (connectorId == "" || source == "" || destination == "" || topic == "") {
      shutdown("Missing required properties (connector, source, destination, topic)")
    }

    log.info("All properties loaded")
    val publisher = new PublisherImpl(destination, port, connectorId, topic)
    val pollPublisher = system.actorOf(Props(new JsonStringURLPoller(publisher)))
    system.eventStream.subscribe(pollPublisher, classOf[String])
    cancellable = system.scheduler.schedule(FiniteDuration.apply(0L, MILLISECONDS),
      FiniteDuration.apply(refreshInterval, MILLISECONDS))(system.eventStream.publish(source))
  } else {
    shutdown("Properties are not set")
  }

  def shutdown(message: String = "", connector: String = mqttPublisher): Unit = {
    super.shutdown(log, message, connector)
    if (cancellable != null)
      cancellable.cancel()
    system.shutdown()
  }
}
