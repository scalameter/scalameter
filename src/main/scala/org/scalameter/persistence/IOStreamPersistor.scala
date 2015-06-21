package org.scalameter
package persistence

import java.io._


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

  protected[scalameter] def fileFor(context: Context) =
    new File(s"$path${File.separator}${context.scope}.${context.curve}.$fileExtension")

  final def load(context: Context): History = {
    val file = fileFor(context)
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

  final def save(context: Context, h: History) {
    path.mkdirs()
    val file = fileFor(context)
    val os = outputStream(file)
    try {
      saveTo(h, os)
    } finally {
      os.close()
    }
  }
}
