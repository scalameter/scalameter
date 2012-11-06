package org.collperf



import java.io._
import sys.process._
import compat.Platform
import utils.withGCNotification
import Key._



package object execution {

  case class Warmer(ctx: Context, setup: () => Any, teardown: () => Any) {
    val minwarmups = ctx.goe(exec.minWarmupRuns, 10)
    val maxwarmups = ctx.goe(exec.maxWarmupRuns, 50)

    def foreach[U](f: Int => U): Unit = {
      val withgc = new utils.SlidingWindow(minwarmups)
      val withoutgc = new utils.SlidingWindow(minwarmups)
      @volatile var nogc = true

      log.verbose(s"Starting warmup.")

      withGCNotification { n =>
        nogc = false
        log.verbose("GC detected.")
      } apply {
        setup()
        var i = 0
        while (i < maxwarmups) {
          nogc = true

          val start = Platform.currentTime
          f(i)
          val end = Platform.currentTime
          val runningtime = end - start

          if (nogc) withoutgc.add(runningtime)
          withgc.add(runningtime)
          teardown()
          setup()

          val covNoGC = withoutgc.cov
          val covGC = withgc.cov

          log.verbose(s"$i. warmup run running time: $runningtime (covNoGC: $covNoGC, covGC: $covGC)")
          if ((withoutgc.size >= minwarmups && covNoGC < 0.1) || (withgc.size >= minwarmups && covGC < 0.1)) {
            log.verbose(s"Steady-state detected.")
            i = maxwarmups
          } else i += 1
        }
        log.verbose(s"Ending warmup.")
      }
    }
  }

  def createJvmContext(ctx: Context, startHeap: Int = 2048, maxHeap: Int = 2048) = {
    val flags = s"${if (initialContext.goe(Key.verbose, false)) "-verbose:gc" else ""} -Xmx${maxHeap}m -Xms${startHeap}m -XX:CompileThreshold=1"
    Context(ctx.properties + (jvmflags -> flags))
  }

  final class JvmRunner {

    private val tmpfile = File.createTempFile("newjvm-", "-io")
    tmpfile.deleteOnExit()

    def run[R](ctx: Context)(body: =>R): R = {
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
      val classpath = ctx.goe(Key.classpath, sys.props("java.class.path"))
      val flags = ctx.goe(Key.jvmflags, "")
      val command = s"java -server $flags -cp $classpath} ${classOf[Main].getName} ${tmpfile.getPath}"
      log.verbose(s"Starting new JVM: $command")
      command !;
    }

    private def readOutput[R](): R = {
      val fis = new FileInputStream(tmpfile)
      val ois = new ObjectInputStream(fis)
      try {
        ois.readObject().asInstanceOf[R]
      } finally {
        fis.close()
        ois.close()
      }
    }

  }

  class Main

  object Main {
    def main(args: Array[String]) {
      val tmpfile = new File(args(0))
      mainMethod(tmpfile)
    }

    def mainMethod(tmpfile: File) {
      val body = loadBody(tmpfile)
      val result = body()
      saveResult(tmpfile, result)
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
  }

}












