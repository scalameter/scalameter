package org.scalameter.execution



import java.io._
import org.scalameter._
import org.scalameter.utils.ClassPath
import scala.util.{Success, Failure, Try}
import scala.sys.process._



final class JvmRunner {

  private val tmpfile = File.createTempFile("newjvm-", "-io")
  tmpfile.deleteOnExit()

  def run[R](ctx: Context)(body: =>R): Try[R] = {
    serializeInput(() => body)
    runJvm(ctx)
    readOutput[R](ctx)
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
    val classpath = ctx(Key.finalClasspath)
    val flags = ctx(Key.exec.jvmflags)
    val jcmd = ctx(Key.exec.jvmcmd)
    val command = Seq(
      jcmd,
      "-server"
    ) ++ flags ++ Seq(
      "-cp",
      classpath.mkString,
      classOf[Main].getName,
      tmpfile.getPath)
      //s"$jcmd $flags -cp $classpath ${classOf[Main].getName} ${tmpfile.getPath}"
    log.verbose(s"Starting new JVM: ${command.mkString(" ")}")
    command.!
  }

  private def readOutput[R](ctx: Context): Try[R] = {
    val fis = new FileInputStream(tmpfile)
    val ois = new ObjectInputStream(fis)
    try {
      val cl = ctx(Key.finalClasspath)
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
