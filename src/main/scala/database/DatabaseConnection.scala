package database
import java.sql.DriverManager
import java.sql.Connection
import com.typesafe.config.Config
import org.apache.logging.log4j.{LogManager, Logger}
import scala.util.{Success, Failure}
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
  val logger = LogManager.getLogger(classOf[DatabaseConnection])

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

    if (connections.tail.isEmpty){
      connections = openConnections
    }

    val conOption = connections.headOption
    connections  = connections.tail
conOption match{
  case Some(con) => con.map {
    z =>
      z.value.map(o => o match {
        case Success(value) => Future(value)
        case Failure(exception) => logger.error(f"Error getting connection to ${uri}"); getConnection
      })
  } match{
    case Some(value) => value match {
      case Some(value) => value
      case None => Thread.sleep(10000); logger.error(f"Error getting connection to ${uri}"); getConnection
    }
    case None => {logger.error({"Connection"}); getConnection}
  }
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
