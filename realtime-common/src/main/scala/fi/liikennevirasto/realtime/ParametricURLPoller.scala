package fi.liikennevirasto.realtime

import java.io.InputStream

abstract class ParametricURLPoller[A](publisher: Publisher) extends URLPoller[A](publisher: Publisher){

  var param: String = ""
  override def pollSource(source: String): InputStream = {
    log.info(System.currentTimeMillis() + ": Connecting to: " + source.format(param))
    fetcher.urlConnection(source.format(param))
  }

  def setParam(newParam: String): Unit = {
    param = newParam
  }
}

