package fi.liikennevirasto.realtime.strategy

import org.json4s.CustomSerializer
import org.json4s.JsonAST.{JLong, JString}

/**
  * Custom Serializer to convert the long values to long instead of string
  */
class LongSerializer extends CustomSerializer[Long](
  format => ( {
    case JString(x) => x.toLong
  }, {
    case x: Long => JLong(x)
  }
  ))