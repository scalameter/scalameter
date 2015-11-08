package java.io

object File {
  val pathSeparator = "/"
  val pathSeparatorChar = '/'
  val separator = pathSeparator

  def createTempFile(prefix: String, suffix: String): File = ???
}

class File(path: String) {
  def getAbsolutePath: String = path
  def exists: Boolean = ???
  def isFile: Boolean = ???

  def mkdirs(): Unit = ???
  def delete() : Unit = ???
  def deleteOnExit() : Unit = ???

}