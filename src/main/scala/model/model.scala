package model



sealed abstract class  ABModel(uid: String, name: String,  target: String, features: List[String], prediction: String, probability: String, id: String, timestamp: String) {

  /*
  A model is a mapping of features to a target
  In our case the model represents the output schema of a table containing the target features prediction and probability
  The UID is the unique identifier of our model
  The Name is the display name
  the target is the name of the target column

   */

  def values: String = {
    var values = List(uid, name, target, prediction, probability, id, timestamp ).filter(x => x!=None)  ++ features

    var statement: String  = values.map(x => "'" + x + "'"). mkString(",")
    s"VALUES ( $statement ) "
  }




}


class Model(uid: String, name: String,  target: String, features: List[String], prediction: String, probability: String, id: String, timestamp: String)

  extends ABModel(uid: String, name: String,  target: String, features: List[String], prediction: String, probability: String, id: String, timestamp: String)

object Model{


  def apply(uid: Option[String], name: Option[String] =  None,  target: Option[String]=None, features: Option[List[String]]=None, prediction: Option[String], probability: Option[String], id: Option[String] = None, timestamp: Option[String] = None): Model = {

    new Model(uid.orNull, name.orNull, target.orNull, features.getOrElse(List.empty), prediction.orNull, probability.orNull, id.orNull, timestamp.orNull)
  }

  def fromList(str: List[Any]): Model ={



    val args = str.map(x=>Option(x) )

    var uid = args(0).asInstanceOf[Option[String]]
    var name = args(1).asInstanceOf[Option[String]]
    var target = args(2).asInstanceOf[Option[String]]
    var features = args(3).asInstanceOf[Option[List[String]]]
    var prediction = args(4).asInstanceOf[Option[String]]
    var probability = args(5).asInstanceOf[Option[String]]
    var id  = args(6).asInstanceOf[Option[String]]
    var timestamp = args(7).asInstanceOf[Option[String]]


    Model( uid, name, target, features, prediction, probability, id, timestamp   )



  }


  def apply(list: List[Any]): Model = {

    return Model.fromList(list)


  }

  def fromMap(map: Map[String, Any]): Model = {


    var uid = map(ModelEnum.uid.toString).asInstanceOf[Option[String]]
    var name = map(ModelEnum.name.toString).asInstanceOf[Option[String]]
    var target = map(ModelEnum.target.toString).asInstanceOf[Option[String]]
    var features = map(ModelEnum.features.toString).asInstanceOf[Option[List[String]]]
    var prediction = map(ModelEnum.prediction.toString).asInstanceOf[Option[String]]
    var probability = map(ModelEnum.probability.toString).asInstanceOf[Option[String]]
    var id  = map(ModelEnum.id.toString).asInstanceOf[Option[String]]
    var timestamp = map(ModelEnum.timestamp.toString).asInstanceOf[Option[String]]


    Model( uid, name, target, features, prediction, probability, id, timestamp   )


  }

  def apply(map: Map[String, Any]): Model = {
    Model.fromMap(map)
  }
}