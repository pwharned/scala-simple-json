package database

object CreateModelsTableStatement extends Statement {

  override val statement: String =
    """
create table models
      |(
      |id integer not null generated always as identity,
      | name varchar(30),
      | model varchar(3200),
      |  primary key(id)
      |  )
      |""".stripMargin

}
