import database.{ApplicationInitializer, Model, Statement}

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object InsertTest extends App with ApplicationInitializer {

  object InsertStatement extends Statement{
    override val statement =
      """
        |insert into transactions(feature_name, feature_value) (select feature_name, feature_value from (
        | With inserts(feature_name, feature_value, rowid ) as (
        |select 'feature1', random, row_number() over() as rowid from sysibm.sysdummy1
        |union all
        |select 'feature1', random, rowid + 1 from inserts
        |where rowid<10)
        |select feature_name, feature_value from inserts)
        |)
        |""".stripMargin
  }

  val start = System.currentTimeMillis()/1000

  Await.result( executeStatement(InsertStatement), Duration.Inf)

  logger.info( f"Finished inserting in ${start - (System.currentTimeMillis()/1000)} seconds" )

  val model: Model = new Model(model_id = 1, model = "test")

  Await.result( executeStatement(model), Duration.Inf)

  logger.info( f"Finished inserting in ${start - (System.currentTimeMillis()/1000)} seconds" )

}
