package org.scalameter
package persistence

import java.io._
import java.util.zip._
import Key.reports._


case class GZIPJSONSerializationPersistor(path: File) extends IOStreamPersistor[GZIPInputStream, GZIPOutputStream] {
  def this(path: String) = this(new File(path))

  def this() = this(currentContext(resultDir))

  def fileExt = "json.gz"

  protected def inputStream(file: File): GZIPInputStream = new GZIPInputStream(new FileInputStream(file))

  protected def outputStream(file: File): GZIPOutputStream = new GZIPOutputStream(new FileOutputStream(file))

  protected def loadFrom(is: GZIPInputStream): History = {
    json.mapper.readValue[History](is)
  }

  protected def saveTo(history: History, os: GZIPOutputStream): Unit = {
    json.mapper.writeValue(os, history)
  }
}

object GZIPJSONSerializationPersistor {
  def apply() = new GZIPJSONSerializationPersistor()
}
