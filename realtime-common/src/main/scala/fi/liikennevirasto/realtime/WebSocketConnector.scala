package fi.liikennevirasto.realtime

import akka.actor.ActorSystem
import akka.{Done, NotUsed}
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.ws._

import scala.concurrent.Future


trait WebSocketConnector extends URLConnector {

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  import system.dispatcher

  // forward each incoming strict text message to the implementing class
  val messageSink: Sink[Message, Future[Done]] =
    Sink.foreach {
      case message: TextMessage.Strict =>
        handleMessage(message.text)
      case message: TextMessage.Streamed =>
        handleMessage(message.getStrictText)
      case _ =>
        println("Received unknown message")
    }

  // the Future[Done] is the materialized value of Sink.foreach
  // and it is completed when the stream completes
  val flow: Flow[Message, Message, Future[Done]] =
  Flow.fromSinkAndSourceMat(messageSink, Source.maybe)(Keep.left)

  // upgradeResponse is a Future[WebSocketUpgradeResponse] that
  // completes or fails when the connection succeeds or fails
  // and closed is a Future[Done] representing the stream completion from above
  val (upgradeResponse, closed) =
  Http().singleWebSocketRequest(WebSocketRequest(source), flow)

  val connected = upgradeResponse.map { upgrade =>
    // just like a regular http request we can get 404 NotFound,
    // with a response body, that will be available from upgrade.response
    if (upgrade.response.status == StatusCodes.OK) {
      Future.successful(Done)
    } else {
      throw new RuntimeException(s"Connection failed: ${upgrade.response.status}")
    }
  }

  // TODO: error handling etc
  connected.onComplete(t => println("Completed " + t.toString))
  closed.foreach(_ => println("closed"))

  def handleMessage(message: String): Unit
}
