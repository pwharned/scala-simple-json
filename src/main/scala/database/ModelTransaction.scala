package database

case class
ModelTransaction(feature_names:
                 Array[String],
                 feature_values: Array[Float]) extends  Insertable