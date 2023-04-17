package database



object CreateTransactionsTableStatement extends Statement {

  override val statement: String =
    """
      CREATE TABLE transactions (
        model_id    INT NOT NULL DEFAULT 1,
        transaction_id int not null ,
        feature_name    varchar(36),
        feature_value    FLOAT,
      scoring_timestamp TIMESTAMP not null with default current_timestamp,
        scoring_date  GENERATED ALWAYS AS (DATE(scoring_timestamp)),
        primary key(transaction_id, feature_name))
          organize by column
      """.stripMargin

}
