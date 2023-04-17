import database.Model

object Insertabletest extends App  {

  val model: Model = new Model(1, "test")

  println(model.insert("models"))



}
