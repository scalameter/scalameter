package org.scalameter.utils

import java.io.File

object ClassPath {
  def default = extract(this.getClass.getClassLoader, sys.props("java.class.path"))

  def extract(classLoader: ClassLoader, default: => String): String =
    classLoader match {
      case urlclassloader: java.net.URLClassLoader => extractFromUrlCL(urlclassloader)
      case null => sys.props("sun.boot.class.path")
      case _ =>
        val parent = classLoader.getParent
        if (parent != null)
          extract(parent, default)
        else
          default
    }

  private def extractFromUrlCL(urlclassloader: java.net.URLClassLoader): String = {
    val fileResource = "file:(.*)".r
    val files = urlclassloader.getURLs.map(_.toString) collect {
      case fileResource(file) => file
    }
    files.mkString(File.pathSeparator)
  }
}
