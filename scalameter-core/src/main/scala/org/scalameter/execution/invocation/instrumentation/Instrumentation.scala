package org.scalameter.execution.invocation.instrumentation

import java.io._
import java.util.jar.{Attributes, JarEntry, JarOutputStream}
import java.util.zip.ZipFile
import org.objectweb.asm.{ClassReader, ClassWriter}
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
  private def filterClasses(in: File, p: String => Boolean): Iterator[(String, InputStream)] = {
    val dirFilter = new FilenameFilter {
      def accept(file: File, s: String): Boolean = new File(file, s).isDirectory
    }

    val classFilter = new FilenameFilter {
      def accept(file: File, s: String): Boolean = s.endsWith(".class")
    }

    def filterClasses(in: File, rootPath: String): Iterator[(String, InputStream)] = {
      val subdirs = in.listFiles(dirFilter)
      val classes = in.listFiles(classFilter)
      val classesFromCurrentDir = classes.iterator.collect {
        case f if p(s"$rootPath/${f.getName}".stripPrefix("/").stripSuffix(".class").replace('/', '.')) =>
          val path = s"$rootPath/${f.getName}".stripPrefix("/")
          path -> new FileInputStream(f)
      }
      val classessFromSubdirs =
        subdirs.iterator.flatMap(f => filterClasses(f, s"$rootPath/${f.getName}".stripPrefix("/")))

      classesFromCurrentDir ++ classessFromSubdirs
    }

    if (!in.exists()) Iterator.empty
    else if (in.isDirectory) {
      filterClasses(in, rootPath = "")
    } else { // if it's not directory try to read it as zip/jar file
      val zipFile = new ZipFile(in)
      zipFile.entries().asScala.collect {
        case entry if entry.getName.endsWith(".class") && p(entry.getName.stripSuffix(".class").replace('/', '.')) =>
          entry.getName -> zipFile.getInputStream(entry)
      }
    }
  }

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
  def writeInstrumentedClasses(ctx: Context, matcher: InvocationCountMatcher, to: File): mutable.ArrayBuffer[MethodSignature] = {
    val lookupTable = mutable.ArrayBuffer.empty[MethodSignature]
    val classpath = ctx.goe(Key.classpath, utils.ClassPath.default)

    val os = new FileOutputStream(to)
    try {
      val manifest = new java.util.jar.Manifest
      manifest.getMainAttributes.put(Attributes.Name.MANIFEST_VERSION, "1.0")

      val jos = new JarOutputStream(os, manifest)
      var currentIndex = 0
      try {
        for (
          path <- classpath.paths.iterator;
          (className, classStream) <- filterClasses(path, matcher.classMatches)
        ) {
          val writer = new ClassWriter(ClassWriter.COMPUTE_MAXS)

          try {
            val classfileBuffer = utils.IO.readFromInputStream(classStream)
            val visitor = new MethodInvocationCounterVisitor(writer, matcher, initialIndex = currentIndex,
              counterClass = classOf[MethodInvocationCounter].getName.replace('.', '/'), counterMethod = "methodCalled")
            val reader = new ClassReader(classfileBuffer)
            reader.accept(visitor, 0)
            lookupTable ++= visitor.methods
            currentIndex = visitor.currentIndex
          } finally {
            classStream.close()
          }

          jos.putNextEntry(new JarEntry(className))
          jos.write(writer.toByteArray)
          jos.closeEntry()
        }
      }
      finally {
        jos.close()
      }
    }
    finally {
      os.close()
    }

    lookupTable
  }
}
