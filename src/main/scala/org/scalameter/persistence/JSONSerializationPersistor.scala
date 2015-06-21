package org.scalameter
package persistence

import java.io._
import Key.reports._
import json._


/** Serializes [[org.scalameter.History]] to JSON using Jackson.
 */
case class JSONSerializationPersistor(path: File) extends IOStreamPersistor[FileInputStream, FileOutputStream] {
  def this(path: String) = this(new File(path))

  def this() = this(currentContext(resultDir))

  def fileExtension = "json"

  protected def inputStream(file: File): FileInputStream = new FileInputStream(file)

  protected def outputStream(file: File): FileOutputStream = new FileOutputStream(file)

  protected def loadFrom(is: FileInputStream): History = {
    jsonMapper.readValue[History](is)
  }

  protected def saveTo(history: History, os: FileOutputStream): Unit = {
    jsonMapper.writeValue(os, history)
  }
}

object JSONSerializationPersistor {
  def apply() = new JSONSerializationPersistor()
}
