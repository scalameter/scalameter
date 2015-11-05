package org.scalameter.execution.invocation.instrumentation

import java.io._
import java.util.jar.{Attributes, JarEntry, JarOutputStream}
import java.util.zip.ZipFile
//import org.objectweb.asm.{ClassReader, ClassWriter}
import scala.collection.convert.decorateAsScala._
import scala.collection.mutable
import org.scalameter._
import org.scalameter.execution.invocation._


private[scalameter] object Instrumentation {
  /** Checks if classes from a given location represented by their classname should be instrumented.
   *
   *  Note that it supports reading from directories and jars.
   *
   *  @param in location of classes to read from
   *  @param p predicate that based on full class name decides if a class should be instrumented
   *  @return [[scala.Iterator]] of tuples in a form of `(fullClassName, instrumentedClassBytes)`
   */
  private def filterClasses(in: File, p: String => Boolean): Iterator[(String, InputStream)] = ???

  /** Writes jar with the instrumented classes to a given file.
   *
   *  Note that if the given [[org.scalameter.Context]] does not contain a classpath,
   *  [[org.scalameter.utils.ClassPath.default]] is used to get a default classpath.
   *
   *  @param ctx [[org.scalameter.Context]] with a classpath key
   *  @param matcher [[org.scalameter.execution.invocation.InvocationCountMatcher]] to match methods that need instrumentation
   *  @param to writes jar with instrumented to a given file
   *  @return lookup table of instrumented methods
   */
  def writeInstrumentedClasses(ctx: Context, matcher: InvocationCountMatcher, to: File): mutable.ArrayBuffer[MethodSignature] = ???
}
