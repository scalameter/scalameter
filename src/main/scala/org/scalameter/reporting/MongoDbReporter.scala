package org.scalameter
package reporting


import org.apache.commons.io._
import org.scalameter.utils.Tree

import scala.sys.process.Process
import spray.json._
import spray.json.DefaultJsonProtocol._
import org.mongodb.scala._
import org.mongodb.scala.bson.BsonDocument

import scala.concurrent.Await
import scala.concurrent.duration.Duration


/** Logs numeric results as MongoDB documents.
 */
case class MongoDbReporter[T: Numeric]() extends Reporter[T] {
  val host = sys.env.getOrElse("MONGODB_REPORTER_URL", "")
  val port = sys.env.getOrElse("MONGODB_REPORTER_PORT", "0").toInt
  val database = sys.env.getOrElse("MONGODB_REPORTER_DATABASE", "")
  val collection = sys.env.getOrElse("MONGODB_REPORTER_COLLECTION", "")
  val gitPropsPath = sys.env.getOrElse("MONGODB_REPORTER_GITPROPS_PATH", "")

  implicit object AnyJsonFormat extends JsonFormat[Any] {
    def write(x: Any) = x match {
      case n: Int => JsNumber(n)
      case s: String => JsString(s)
      case x: Seq[_] => seqFormat[Any].write(x)
      case m: Map[String, Any] @unchecked => mapFormat[String, Any].write(m)
      case b: Boolean if b == true => JsTrue
      case b: Boolean if b == false => JsFalse
      case _ => sys.error("cannot write " + x)
    }
    def read(value: JsValue) = value match {
      case JsNumber(n) => n.intValue()
      case JsString(s) => s
      case a: JsArray => listFormat[Any].read(value)
      case o: JsObject => mapFormat[String, Any].read(value)
      case JsTrue => true
      case JsFalse => false
      case _ => sys.error("cannot read " + value)
    }
  }

  def report(result: CurveData[T], persistor: Persistor): Unit = {
  }

  def report(result: Tree[CurveData[T]], persistor: Persistor): Boolean = {
    if (host == "") return true

    val client = MongoClient(s"mongodb://$host:$port")
    val db = client.getDatabase(database)
    val coll = db.getCollection(collection)

    val gitprops = IOUtils.toString(getClass.getResourceAsStream(gitPropsPath), "utf-8")
      .parseJson.convertTo[Map[String, Any]]
    val hostname = Process("hostname").!!.trim

    for (curve <- result; measurement <- curve.measurements) {
      val tuples = Seq(
        "scope" -> curve.context.scope,
        "curve" -> curve.context.curve,
        "machine:hostname" -> hostname,
        "commit:rev" -> gitprops("sha").toString,
        "commit:commit-ts" -> gitprops("commit-timestamp").toString,
        "commit:branch" -> gitprops("branch").toString,
        "metric:value" -> measurement.value.toString
      ) ++ measurement.params.axisData.map {
        case (p, v) => ("metric:param:" + p.fullName) -> v
      }

      val underlying = new BsonDocument()
      tuples.foreach(elem => underlying.put(elem._1, elem._2))
      val doc = Document(underlying)
      val observable: Observable[Completed] = coll.insertOne(doc)
      Await.result(observable.toFuture(), atMost = Duration.Inf)
    }

    true
  }
}
