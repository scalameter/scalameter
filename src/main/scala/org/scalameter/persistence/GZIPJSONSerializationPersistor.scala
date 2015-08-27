package org.scalameter
package persistence



import java.io._
import java.util.zip._
import org.scalameter.Key.reports._
import org.scalameter.persistence.json._



/** Serializes [[org.scalameter.History]] to GZIP JSON using Jackson.
  */
case class GZIPJSONSerializationPersistor(path: File)
  extends IOStreamPersistor[GZIPInputStream, GZIPOutputStream] {
  def this(path: String) = this(new File(path))

  def this() = this(currentContext(resultDir))

  def fileExtension = "json.gz"

  protected def inputStream(file: File): GZIPInputStream =
    new GZIPInputStream(new FileInputStream(file))

  protected def outputStream(file: File): GZIPOutputStream =
    new GZIPOutputStream(new FileOutputStream(file))

  protected def loadFrom[T](is: GZIPInputStream): History[T] = {
    jsonMapper.readValue[History[_]](is).asInstanceOf[History[T]]
  }

  protected def saveTo[T](history: History[T], os: GZIPOutputStream): Unit = {
    jsonMapper.writeValue(os, history)
  }
}

object GZIPJSONSerializationPersistor {
  def apply() = new GZIPJSONSerializationPersistor()
}
