package fi.liikennevirasto.realtime.strategy

trait ForwardStrategy {
  def handleMessage(connectorName: String, payload: Array[Byte]): Array[Byte]
}
