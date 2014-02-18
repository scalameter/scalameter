package org.scalameter



import language.postfixOps
import java.io._
import sys.process._
import compat.Platform
import Key._
import scala.util.{Try, Success, Failure}



package object execution {

  def createJvmContext(ctx: Context) = {
    val existingFlags = ctx(exec.jvmflags)
    val flags = s"${if (initialContext(Key.verbose)) "-verbose:gc" else ""} " + existingFlags
    ctx + (exec.jvmflags -> flags)
  }

  final class JvmRunner {

    private val tmpfile = File.createTempFile("newjvm-", "-io")
    tmpfile.deleteOnExit()

    def run[R](ctx: Context)(body: =>R): Try[R] = {
      serializeInput(() => body)
      runJvm(ctx)
      readOutput[R]()
    }

    private def serializeInput[T](config: T) {
      val fos = new FileOutputStream(tmpfile)
      val oos = new ObjectOutputStream(fos)
      try {
        oos.writeObject(config)
      } finally {
        fos.close()
        oos.close()
      }
    }

    private def runJvm(ctx: Context) {
      val classpath = ctx.goe(Key.classpath, defaultClasspath)
      val flags = ctx(Key.exec.jvmflags)
      val jcmd = ctx(Key.exec.jvmcmd)
      val command = s"$jcmd $flags -cp $classpath ${classOf[Main].getName} ${tmpfile.getPath}"
      log.verbose(s"Starting new JVM: $command")
      command !;
    }

    private def readOutput[R](): Try[R] = {
      val fis = new FileInputStream(tmpfile)
      val ois = new ObjectInputStream(fis)
      try {
        val result = ois.readObject()
        result match {
          case SeparateJvmFailure(t) => Failure(t)
          case result => Success(result.asInstanceOf[R])
        }
      } finally {
        fis.close()
        ois.close()
      }
    }

  }
  
  case class SeparateJvmFailure(t: Throwable)

  class Main

  object Main {
    def main(args: Array[String]) {
      val tmpfile = new File(args(0))
      mainMethod(tmpfile)
    }

    def mainMethod(tmpfile: File) {
      try {
        val body = loadBody(tmpfile)
        val result = body()
        saveResult(tmpfile, result)
      } catch {
        case t: Throwable =>
          saveFailure(tmpfile, t)
      }
    }

    private def loadBody(file: File): () => Any = {
      val fis = new FileInputStream(file)
      val ois = new ObjectInputStream(fis)
      try {
        ois.readObject().asInstanceOf[() => Any]
      } finally {
        fis.close()
        ois.close()
      }
    }

    private def saveResult[R](file: File, result: R) {
      val fos = new FileOutputStream(file)
      val oos = new ObjectOutputStream(fos)
      try {
        oos.writeObject(result)
      } finally {
        fos.close()
        oos.close()
      }
    }

    private def saveFailure(file: File, t: Throwable) {
      val fos = new FileOutputStream(file)
      val oos = new ObjectOutputStream(fos)
      try {
        oos.writeObject(SeparateJvmFailure(t))
      } finally {
        fos.close()
        oos.close()
      }
    }
  }

}












