package database

trait Insertable extends Statement with Product {
  def insert(table: String): String = f"insert into ${table} ( ${productElementNames.mkString(",")} ) values (${productIterator.mkString(",")})"

}