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

  protected def loadFrom[T](is: I): History[T]

  protected def saveTo[T](history: History[T], os: O): Unit

  protected[scalameter] def fileFor(context: Context) =
    new File(s"$path${File.separator}${context.scope}.${context.curve}.$fileExtension")

  final def load[T](context: Context): History[T] = {
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

  final def save[T](context: Context, h: History[T]) {
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
