package org.scalameter
package persistence

import java.io._
import Key.reports._


case class JSONSerializationPersistor(path: File) extends IOStreamPersistor[FileInputStream, FileOutputStream] {
  def this(path: String) = this(new File(path))

  def this() = this(currentContext(resultDir))

  def fileExt = "json"

  protected def inputStream(file: File): FileInputStream = new FileInputStream(file)

  protected def outputStream(file: File): FileOutputStream = new FileOutputStream(file)

  protected def loadFrom(is: FileInputStream): History = {
    json.mapper.readValue[History](is)
  }

  protected def saveTo(history: History, os: FileOutputStream): Unit = {
    json.mapper.writeValue(os, history)
  }
}

object JSONSerializationPersistor {
  def apply() = new JSONSerializationPersistor()
}
