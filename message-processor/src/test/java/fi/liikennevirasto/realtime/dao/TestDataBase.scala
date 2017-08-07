package fi.liikennevirasto.realtime.dao

import java.util.Properties

import slick.dbio.{DBIOAction, NoStream}
import slick.jdbc.PostgresProfile
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

/**
  * Created by venholat on 3.4.2017.
  */
object TestDataBase extends DataBase {
  def isAvailable: Boolean = {
    val test = sql"""SELECT now()""".as[String].headOption
    try {
      Await.result(withSession(test), atMost=Duration.create(15, "seconds")).nonEmpty
    } catch {
      case t: Throwable => false
    }
  }

  lazy val properties: Properties = {
    val props = new Properties()
    props.load(getClass.getResourceAsStream("/realtime.properties"))
    props
  }
  private val databaseUrl = properties.getProperty("realtime.database")
  private val user = properties.getProperty("user")
  private val pass = properties.getProperty("password")

  private lazy val connectionURL = databaseUrl + "?user=" + user + "&password=" + pass
  lazy val db: PostgresProfile.backend.DatabaseDef = Database.forURL(url = connectionURL, driver="org.postgresql.Driver", executor=executor)

  def getConnectionURL: String = connectionURL
  /*
  Usage:
  val action = sql"select id from foo where bar".as[Long]
  val future: Future[Long] = DataBase.withSession(action)
  val result = Await.result(future, 2 seconds)
   */
  def withSession[R](action: DBIOAction[R, NoStream, Nothing]) : Future[R] = {
    val session = db.createSession()
    try {
      db.run(action)
    } finally {
      session.close()
    }
  }

  def withTransaction[R](action: DBIOAction[R, NoStream, Nothing with Effect.Transactional]) : Future[R] = {
    val session = db.createSession()
    db.executor.executionContext.prepare()
    try {
      db.run(action.transactionally)
    } finally {
      session.close()
    }
  }
}
