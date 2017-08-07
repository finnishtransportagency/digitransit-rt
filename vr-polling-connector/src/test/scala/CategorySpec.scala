import fi.liikennevirasto.realtime.{Category, DetailedCategory}
import org.json4s.JsonAST.JObject
import org.json4s.jackson.JsonMethods
import org.json4s.{DefaultFormats, Formats, StringInput}
import org.scalatest.{FunSuite, Matchers}

import scala.io.Source

class CategorySpec extends FunSuite with Matchers{

  test("Detailed categories should match their upper level categories") {
    DetailedCategory.values.forall(dcc => dcc.id == 0 || dcc.detailedCategoryCode.startsWith(dcc.categoryGroup.categoryCode)) should be (true)
  }

  test("All categories used have detailed categories") {
    Category.values.forall(c => c.id == 0 || DetailedCategory.values.exists(_.categoryGroup == c)) should be (true)
  }

  test("All ids are unique") {
    Category.values.groupBy(_.id).forall{case (_, s) => s.size == 1 } should be (true)
    Category.values.groupBy(_.categoryCode).forall{case (_, s) => s.size == 1 } should be (true)
    Category.values.groupBy(_.categoryName).forall{case (_, s) => s.size == 1 } should be (true)
    DetailedCategory.values.groupBy(_.id).forall{case (_, s) => s.size == 1 } should be (true)
    DetailedCategory.values.groupBy(_.detailedCategoryCode).forall{case (_, s) => s.size == 1 } should be (true)
    DetailedCategory.values.groupBy(_.detailedCategoryName).forall{case (_, s) => s.size == 1 } should be (true)
  }

  test("Fetch JSON from API and cross check") {
    implicit val jsonFormats: Formats = DefaultFormats

    val cause = "https://rata.digitraffic.fi/api/v1/metadata/cause-category-codes"
    val detailedCause = "https://rata.digitraffic.fi/api/v1/metadata/detailed-cause-category-codes"
    val causeJSON = Source.fromURL(cause).mkString("")
    val parsedCause = JsonMethods.parse(StringInput(causeJSON))
    parsedCause.extract[Seq[JObject]].map(_.values).foreach{
      jo =>
        val dcc = Category.apply(jo.getOrElse("categoryCode", "").asInstanceOf[String])
        dcc shouldNot be (Category.Unknown)
        jo.get("id") should be (Some(dcc.id))
        jo.get("categoryName") should be (Some(dcc.categoryName))
    }

    val detailedCauseJSON = Source.fromURL(detailedCause).mkString("")
    val parsedDetailedCause = JsonMethods.parse(StringInput(detailedCauseJSON))
    parsedDetailedCause.extract[Seq[JObject]].map(_.values).foreach{
      jo =>
        val dcc = DetailedCategory.apply(jo.getOrElse("detailedCategoryCode", "").asInstanceOf[String])
        dcc shouldNot be (DetailedCategory.Unknown)
        jo.get("id") should be (Some(dcc.id))
        jo.get("detailedCategoryName") should be (Some(dcc.detailedCategoryName))
    }
  }
}
