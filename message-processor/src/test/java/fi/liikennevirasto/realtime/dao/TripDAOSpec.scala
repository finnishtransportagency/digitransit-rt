package fi.liikennevirasto.realtime.dao

import org.scalatest
import org.scalatest.{FunSuite, Ignore, Matchers, Tag}

object dbIsAvailable extends Tag(if (TestDataBase.isAvailable) "" else classOf[Ignore].getName)

class TripDAOSpec extends FunSuite with Matchers {

  lazy val db = TestDataBase

  val dao = new TripDAO(db)


  test("SQL queries should be successful")  {
    val result = dao.getSQLTest("foo")
    result.isEmpty should be (false)
  }

  test("SQL trip conversion") {
    val result = dao.getTrip("oulu", "23AB", "0000392001307051")
    result.isEmpty should be (false)
  }
}
