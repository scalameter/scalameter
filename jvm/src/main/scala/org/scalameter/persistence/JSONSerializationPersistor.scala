package org.scalameter
package persistence



import java.io._
import org.scalameter.Key.reports._
import org.scalameter.persistence.json._



/** Serializes [[org.scalameter.History]] to JSON using Jackson.
 */
case class JSONSerializationPersistor(path: File)
  extends IOStreamPersistor[FileInputStream, FileOutputStream] {
  def this(path: String) = this(new File(path))

  def this() = this(currentContext(resultDir))

  def fileExtension = "json"

  protected def inputStream(file: File): FileInputStream = new FileInputStream(file)

  protected def outputStream(file: File): FileOutputStream = new FileOutputStream(file)

  protected def loadFrom[T](is: FileInputStream): History[T] = {
    jsonMapper.readValue[History[_]](is).asInstanceOf[History[T]]
  }

  protected def saveTo[T](history: History[T], os: FileOutputStream): Unit = {
    jsonMapper.writeValue(os, history)
  }
}

object JSONSerializationPersistor {
  def apply() = new JSONSerializationPersistor()
}
