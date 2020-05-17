package org.scalameter.utils



import java.io.File
import java.net._
import io.github.classgraph.ClassGraph
import org.apache.commons.lang3.SystemUtils
import scala.collection.JavaConverters._
import scala.collection.Seq

class ClassPath private (val paths: Seq[File]) extends Serializable {
  /** Returns platform dependent classpath string.
   */
  def mkString: String = paths.map(_.getAbsolutePath).mkString(File.pathSeparator)

  /** Prepends jar or directory containing classes to the classpath.
   */
  def +:(location: File) = {
    ClassPath.validate(location)
    new ClassPath(location +: paths)
  }

  override def equals(obj: Any): Boolean = obj match {
    case cl: ClassPath =>
      paths.map(_.getAbsolutePath) == cl.paths.map(_.getAbsolutePath)
    case _ =>
      false
  }

  override def hashCode(): Int = paths.map(_.getAbsolutePath).hashCode()

  override def toString = s"ClassPath(${paths.mkString(":")})"
}

object ClassPath {
  private def validate(location: File) = {
    val elem = location.getAbsolutePath
    require(!elem.contains(File.pathSeparatorChar),
      s"Classpath element contains illegal character: ${File.pathSeparatorChar}")
    if (SystemUtils.IS_OS_WINDOWS) {
      require(!elem.contains("\""), "Classpath element contains illegal character: \"")
    }
  }

  private def fromString(classPath: String): ClassPath = {
    ClassPath(classPath.split(File.pathSeparator).map(new File(_)))
  }

  /** Constructs [[ClassPath]] from given list of strings validating them first.
   */
  def apply(paths: Seq[File]): ClassPath = {
    paths.foreach(validate)
    new ClassPath(paths)
  }

  /** Returns the default classpath string.
   */
  def default: ClassPath = {
    extract(this.getClass.getClassLoader, sys.props("java.class.path"))
  }

  /** Extracts the classpath from the classLoader, using the classgraph library.
   */
  def extract(classLoader: ClassLoader, default: => String): ClassPath =
    classLoader match {
      case null =>
        fromString(sys.props("sun.boot.class.path"))
      case classloader =>
        ClassPath(
          extractFileClasspaths(new ClassGraph().addClassLoader(classloader).getClasspathURIs().asScala.map(_.toURL)))
    }

  private[scalameter] def extractFileClasspaths(urls: Seq[URL]): Seq[File] = {
    val fileResource = "file:(.*)".r
    urls.map(s => URLDecoder.decode(s.toString, "UTF-8")) collect {
      case orig @ fileResource(file) => new File(file)
    }
  }
}
