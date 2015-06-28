package org.scalameter.utils



import java.io.File
import java.net._



object ClassPath {
  /** Returns the default classpath string, surrounded by quotes.
   */
  def default: String = {
    extract(this.getClass.getClassLoader, sys.props("java.class.path"))
  }

  /** Extracts the classpath from the given `classLoader` if it is a `URLClassLoader` or
   *  from the first parent that is a `URLClassLoader`.
   *  If no `URLClassLoader` can be found, returns the `default` classpath.
   */
  def extract(classLoader: ClassLoader, default: => String): String =
    classLoader match {
      case urlclassloader: URLClassLoader => extractFromUrlCL(urlclassloader)
      case null => sys.props("sun.boot.class.path")
      case _ =>
        val parent = classLoader.getParent
        if (parent != null)
          extract(parent, default)
        else
          "\"" + default + "\""
    }

  private def extractFromUrlCL(urlclassloader: URLClassLoader): String = {
    val files = extractFileClasspaths(urlclassloader.getURLs)
    files.mkString("\"", File.pathSeparator, "\"")
  }

  private[scalameter] def extractFileClasspaths(urls: Seq[URL]): Seq[String] = {
    val fileResource = "file:(.*)".r
    urls.map(s => URLDecoder.decode(s.toString, "UTF-8")) collect {
      case orig @ fileResource(file) => file
    }
  }
}
