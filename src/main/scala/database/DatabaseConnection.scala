package database
import java.sql.DriverManager
import java.sql.Connection
import com.typesafe.config.Config
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.reflect.runtime.universe.Try

class DatabaseConnection(conf: Config) {
  /*
  host
  port
  user
  passowrd
   */
  val logger = LoggerFactory.getLogger(classOf[DatabaseConnection])

  val driver = "com.ibm.db2.jcc.DB2Driver"


  try{
    Class.forName(driver)
  } catch {
    case e => logger.error(e.getStackTrace.mkString("/n"))
  }

  def username: String = conf.getString("database.user")
  def password: String = conf.getString("database.password")

  def uri: String = {

        val arr: Array[String] = Array("user", "password", "host","port", "db").map(x => conf.getString(f"database.${x}"))

        f"jdbc:db2://${arr(2)}:${arr(3)}/${arr(4)}"

      }

  def openConnection: Option[Future[Connection]] =  {
    Option(Future(DriverManager.getConnection(uri, username, password)).andThen{
      case _ => logger.info(f"Opening connection to ${uri}");getConnection
    }   )
  }

  var connections = openConnections

  def openConnections: Seq[Option[Future[Connection]]] = (0 to 10).map {
    x => { openConnection}
  }

  def getConnection: Future[Connection] = {

    val con = connections.head
    connections = connections.tail
    con match {
      case Some(conn)=> conn
      case e: Throwable => {logger.error(f"Could not get connection to ${uri}"); getConnection }
    }
  }

  def updateConnections: Unit = {
    closeConnections;
    connections = openConnections
  }

  def closeConnections: Unit ={
    this.connections.map{
      x => x.map{
        y => y.map{
          z => z.close()
        }
      }
    }
  }


}
