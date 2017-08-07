package fi.liikennevirasto.realtime

trait Publisher {
  val host: String
  val port: String
  val from: String
  val sendTo: String

  def publish(message: String): Unit
}

