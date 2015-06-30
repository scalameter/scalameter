package org.scalameter.utils



import java.io.File
import java.net._
import org.apache.commons.lang3.SystemUtils



object ClassPath {
  /** Returns the default classpath string.
   */
  def default: String = {
    extract(this.getClass.getClassLoader, sys.props("java.class.path"))
  }

  /** Returns the default classpath string, surrounded by quotes on Windows.
   */
  def platformSpecificDefault: String = {
    platformSpecificExtract(this.getClass.getClassLoader, sys.props("java.class.path"))
  }

  /** Same as `extract`, but returns a string surrounded with quotes on Windows.
   */
  def platformSpecificExtract(classLoader: ClassLoader, default: =>String): String = {
    val cp = extract(classLoader, default)
    if (SystemUtils.IS_OS_WINDOWS) "\"" + cp + "\"" else cp
  }

  /** Extracts the classpath from the given `classLoader` if it is a `URLClassLoader` or
   *  from the first parent that is a `URLClassLoader`.
   *  If no `URLClassLoader` can be found, returns the `default` classpath.
   */
  def extract(classLoader: ClassLoader, default: =>String): String =
    classLoader match {
      case urlclassloader: URLClassLoader => extractFromUrlCL(urlclassloader)
      case null => sys.props("sun.boot.class.path")
      case _ =>
        val parent = classLoader.getParent
        if (parent != null)
          extract(parent, default)
        else
          default
    }

  private def extractFromUrlCL(urlclassloader: URLClassLoader): String = {
    val files = extractFileClasspaths(urlclassloader.getURLs)
    files.mkString(File.pathSeparator)
  }

  private[scalameter] def extractFileClasspaths(urls: Seq[URL]): Seq[String] = {
    val fileResource = "file:(.*)".r
    urls.map(s => URLDecoder.decode(s.toString, "UTF-8")) collect {
      case orig @ fileResource(file) => file
    }
  }
}
