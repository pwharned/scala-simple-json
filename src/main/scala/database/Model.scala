package database



case class Model(model_id: Int, model: String) extends Insertable{
  override val statement: String = insert("models")
}
