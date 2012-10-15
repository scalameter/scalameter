package org.collperf
package execution



import java.io._
import collection._
import sys.process._
import utils.Tree



class NewJvmExecutor(val aggregator: Aggregator, val measurer: Executor.Measurer) extends Executor {

  private val tmpfile = File.createTempFile("newjvm-", "-io")
  tmpfile.deleteOnExit()

  def run[T](setuptree: Tree[Setup[T]]): Tree[CurveData] = {
    for (setup <- setuptree) yield run(setup)
  }

  private def run[T](setup: Setup[T]): CurveData = {
    // serialize setup to tmp file
    serializeInput(NewJvmExecutor.Config(initialContext, aggregator, measurer, setup))

    // run separate JVM
    runJvm()

    // read results from the tmp file
    val curve = readOutput()

    curve
  }

  private def serializeInput[T](config: NewJvmExecutor.Config[T]) {
    val fos = new FileOutputStream(tmpfile)
    val oos = new ObjectOutputStream(fos)
    try {
      oos.writeObject(config)
    } finally {
      fos.close()
      oos.close()
    }
  }

  private def runJvm() {
    val flags = if (initialContext.goe(Key.verbose, false)) "-verbose:gc" else ""
    val command = s"java -server $flags -cp ${sys.props("java.class.path")} ${classOf[NewJvmExecutor].getName} ${tmpfile.getPath}"
    log.verbose(s"Starting new JVM: $command")
    command !;
  }

  private def readOutput(): CurveData = {
    val fis = new FileInputStream(tmpfile)
    val ois = new ObjectInputStream(fis)
    try {
      ois.readObject().asInstanceOf[CurveData]
    } finally {
      fis.close()
      ois.close()
    }
  }

  override def toString = s"NewJvmExecutor(${aggregator.name}, ${measurer.name})"

}


object NewJvmExecutor extends Executor.Factory[NewJvmExecutor] {

  def apply(agg: Aggregator, m: Executor.Measurer) = new NewJvmExecutor(agg, m)

  def main(args: Array[String]) {
    val tmpfile = new File(args(0))
    Main.main(tmpfile)
  }

  case class Config[T](initial: Context, aggregator: Aggregator, measurer: Executor.Measurer, setup: Setup[T])

  object Main {
    def main(tmpfile: File) {
      val setup = loadSetup(tmpfile)
      val curve = run(setup)
      saveCurve(tmpfile, curve)
    }

    private def loadSetup(file: File): Config[Any] = {
      val fis = new FileInputStream(file)
      val ois = new ObjectInputStream(fis)
      try {
        ois.readObject().asInstanceOf[Config[Any]]
      } finally {
        fis.close()
        ois.close()
      }
    }

    private def run[T](config: Config[T]): CurveData = {
      initialContext = config.initial
      val delegate = new LocalExecutor(config.aggregator, config.measurer)
      delegate.runSingle(config.setup)
    }

    private def saveCurve(file: File, curve: CurveData) {
      val fos = new FileOutputStream(file)
      val oos = new ObjectOutputStream(fos)
      try {
        oos.writeObject(curve)
      } finally {
        fos.close()
        oos.close()
      }
    }
  }

}

























