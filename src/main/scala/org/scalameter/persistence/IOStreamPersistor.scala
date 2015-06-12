package org.scalameter
package persistence

import java.io._
import Key.reports._


trait IOStreamPersistor[In <: InputStream, Out <: OutputStream] extends Persistor {
  def path: File

  def fileExt: String

  protected def inputStream(file: File): In

  protected def outputStream(file: File): Out

  protected def loadFrom(is: In): History

  protected def saveTo(history: History, os: Out): Unit

  private def sep: String = File.separator

  private def loadHistory(dir: String, scope: String, curve: String): History = {
    val file = new File(s"$path$sep$scope.$curve.$fileExt")
    if (!file.exists || !file.isFile) History(Nil)
    else {
      val is = inputStream(file)
      try {
        loadFrom(is)
      } finally {
        is.close()
      }
    }
  }

  private def saveHistory(dir: String, scope: String, curve: String, h: History) {
    path.mkdirs()
    val file = new File(s"$path$sep$scope.$curve.$fileExt")
    val os = outputStream(file)
    try {
      saveTo(h, os)
    } finally {
      os.close()
    }
  }

  def load(context: Context): History = {
    val scope = context.scope
    val curve = context.curve
    val resultdir = context(resultDir)
    loadHistory(resultdir, scope, curve)
  }

  def save(context: Context, h: History) {
    val scope = context.scope
    val curve = context.curve
    val resultdir = context(resultDir)
    saveHistory(resultdir, scope, curve, h)
  }
}
