package database

import com.typesafe.config.{Config, ConfigFactory}

trait ApplicationConfiguration {

  val conf: Config = ConfigFactory.load("application.conf");
  val dbconf: DatabaseConnection = new DatabaseConnection(conf)

}
