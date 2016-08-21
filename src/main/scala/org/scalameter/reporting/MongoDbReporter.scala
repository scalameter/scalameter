package org.scalameter
package reporting



import com.mongodb.casbah.Imports._
import org.apache.commons.io._
import org.scalameter.utils.Tree
import scala.annotation.unchecked
import scala.sys.process.Process
import spray.json._
import spray.json.DefaultJsonProtocol._



/** Logs numeric results as MongoDB documents.
 */
case class MongoDbReporter[T: Numeric]() extends Reporter[T] {
  val url = sys.env("MONGODB_REPORTER_URL")
  val port = sys.env("MONGODB_REPORTER_PORT").toInt
  val database = sys.env("MONGODB_REPORTER_DATABASE")
  val collection = sys.env("MONGODB_REPORTER_COLLECTION")
  val gitPropsPath = sys.env("MONGODB_REPORTER_GITPROPS_PATH")

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

  def report(result: CurveData[T], persistor: Persistor) {
  }

  def report(result: Tree[CurveData[T]], persistor: Persistor): Boolean = {
    val client = MongoClient(url, port)
    val db = client(database)
    val coll = db(collection)

    val gitprops = IOUtils.toString(getClass.getResourceAsStream(gitPropsPath), "utf-8")
      .parseJson.convertTo[Map[String, Any]]
    val hostname = Process("hostname").!!.trim

    for (curve <- result; measurement <- curve.measurements) {
      val tuples = List(
        "scope" -> curve.context.scope,
        "curve" -> curve.context.curve,
        "machine:hostname" -> hostname,
        "commit:rev" -> gitprops("sha"),
        "commit:commit-ts" -> gitprops("commit-timestamp"),
        "metric:value" -> measurement.value
      ) ++ measurement.params.axisData.map {
        case (p, v) => ("metric:param:" + p.fullName) -> v
      }
      val doc = MongoDBObject(tuples)
      coll.insert(doc)
    }

    true
  }
}
