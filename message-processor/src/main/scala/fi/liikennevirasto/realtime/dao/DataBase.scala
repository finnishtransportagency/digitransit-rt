package fi.liikennevirasto.realtime.dao

import java.sql._
import java.util.concurrent.TimeUnit

import com.github.tminglei.slickpg._
import fi.liikennevirasto.realtime.MessageProcessor
import play.api.libs.json.{JsValue, Json}
import slick.basic.Capability
import slick.dbio.{DBIOAction, NoStream}
import slick.jdbc.{JdbcCapabilities, PostgresProfile}
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}


trait DataBase extends ExPostgresProfile
  with PgArraySupport
  with PgDate2Support
  with PgRangeSupport
  with PgHStoreSupport
  with PgPlayJsonSupport
  with PgSearchSupport
  with PgPostGISSupport
  with PgNetSupport
  with PgLTreeSupport {
  def pgjson = "jsonb"

  override protected def computeCapabilities: Set[Capability] =
    super.computeCapabilities + JdbcCapabilities.insertOrUpdate

  override val api = MyAPI

  object MyAPI extends API with ArrayImplicits
    with DateTimeImplicits
    with JsonImplicits
    with NetImplicits
    with LTreeImplicits
    with RangeImplicits
    with HStoreImplicits
    with SearchImplicits
    with SearchAssistants {
    implicit val strListTypeMapper = new SimpleArrayJdbcType[String]("text").to(_.toList)
    implicit val playJsonArrayTypeMapper =
      new AdvancedArrayJdbcType[JsValue](pgjson,
        (s) => utils.SimpleArrayUtils.fromString[JsValue](Json.parse)(s).orNull,
        (v) => utils.SimpleArrayUtils.mkString[JsValue](_.toString())(v)
      ).to(_.toList)
  }

  val executor = AsyncExecutor.apply("myexec", 5, 15, -1, 15, Duration.apply(10, TimeUnit.SECONDS), true)
  implicit val executionContext: ExecutionContext = executor.executionContext

  def withSession[R](action: DBIOAction[R, NoStream, Nothing]): Future[R]

  def withTransaction[R](action: DBIOAction[R, NoStream, Nothing with Effect.Transactional]): Future[R]
}

object DataBase extends DataBase {

  private val databaseUrl = MessageProcessor.properties.getProperty("realtime.database")
  private val user = MessageProcessor.properties.getProperty("user")
  private val pass = MessageProcessor.properties.getProperty("password")

  private lazy val connectionURL = databaseUrl + "?user=" + user + "&password=" + pass
  lazy val db: PostgresProfile.backend.DatabaseDef = Database.forURL(url = connectionURL, driver = "org.postgresql.Driver", executor = executor)

  def getConnectionURL: String = connectionURL

  /*
  Usage:
  val action = sql"select id from foo where bar".as[Long]
  val future: Future[Long] = DataBase.withSession(action)
  val result = Await.result(future, 2 seconds)
   */
  def withSession[R](action: DBIOAction[R, NoStream, Nothing]): Future[R] = {
    val session = db.createSession()
    try {
      db.run(action)
    } finally {
      session.close()
    }
  }

  def withTransaction[R](action: DBIOAction[R, NoStream, Nothing with Effect.Transactional]): Future[R] = {
    val session = db.createSession()
    db.executor.executionContext.prepare()
    try {
      db.run(action.transactionally)
    } finally {
      session.close()
    }
  }

  def withPreparedStatement(conn: Connection, query: String, values: Seq[Any]): ResultSet = {
    executeQuery(conn,query,values)
  }

  def insertWithPreparedStatement(conn: Connection, query: String, values: Seq[Any]): Boolean = {
    insert(conn,query,values)
  }


  private def fillValues(values: Seq[Any], preparedStatement: PreparedStatement): PreparedStatement = {
    values.zipWithIndex.map(t => (t._1, t._2 + 1)).foreach {
      case (int: Int, index) => preparedStatement.setInt(index, int)
      case (long: Long, index) => preparedStatement.setLong(index, long)
      case (float: Float, index) => preparedStatement.setFloat(index, float)
      case (string: String, index) => preparedStatement.setString(index, string)
      case (date: Date, index) => preparedStatement.setDate(index,date)
    }
    preparedStatement
  }

  def executeQuery(conn: Connection, query: String, values: Seq[Any]): ResultSet =
    conn.prepareStatement(query) match {
      case preparedStatement: PreparedStatement => fillValues(values, preparedStatement).executeQuery()
    }

  def insert(conn: Connection, query: String, values: Seq[Any]): Boolean =
    conn.prepareStatement(query) match {
      case preparedStatement: PreparedStatement => fillValues(values, preparedStatement).execute
    }

  def createConnection:Connection = {
    db.source.createConnection
  }
}
