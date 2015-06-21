package org.scalameter
package persistence

import java.io._
import Key.reports._


/** Base for persistors that actually write and read [[org.scalameter.History]].
 *
 * @tparam I input stream to which [[org.scalameter.History]] is serialized
 * @tparam O output stream from which [[org.scalameter.History]] is deserialized
 */
trait IOStreamPersistor[I <: InputStream, O <: OutputStream] extends Persistor {
  def path: File

  def fileExtension: String

  protected def inputStream(file: File): I

  protected def outputStream(file: File): O

  protected def loadFrom(is: I): History

  protected def saveTo(history: History, os: O): Unit

  private def directorySeparator: String = File.separator

  private def loadHistory(scope: String, curve: String): History = {
    val file = new File(s"$path$directorySeparator$scope.$curve.$fileExtension")
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

  private def saveHistory(scope: String, curve: String, h: History) {
    path.mkdirs()
    val file = new File(s"$path$directorySeparator$scope.$curve.$fileExtension")
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
    loadHistory(scope, curve)
  }

  def save(context: Context, h: History) {
    val scope = context.scope
    val curve = context.curve
    saveHistory(scope, curve, h)
  }
}
