package org.scalameter



import java.util.ArrayList
import java.util.Timer
import java.util.TimerTask
import org.apache.commons.lang3.time.DurationFormatUtils
import org.jline.terminal.TerminalBuilder
import org.jline.utils.AttributedString
import org.jline.utils.AttributedStyle
import org.jline.utils.Display



abstract class Log {
  def error(msg: String): Unit
  def warn(msg: String): Unit
  def info(msg: String): Unit
  def debug(msg: String): Unit

  def overallBegin(): Unit = {}
  def overallProgress(percent: Double): Unit = {}
  def overallBenchmark(benchmark: String): Unit = {}
  def currentBegin(): Unit = {}
  def currentProgress(percent: Double): Unit = {}
  def currentForkIndex(n: Int): Unit = {}
  def currentTotalForks(n: Int): Unit = {}
  def currentForkCommand(cmd: String): Unit = {}
  def currentInput(input: String): Unit = {}
  def timer(enable: Boolean): Unit = {}
  def clear(): Unit = {}

  def verbose(msg: =>Any) = debug(msg.toString)
  def apply(msg: =>Any) = info(msg.toString)
}


object Log {
  case object None extends Log {
    def error(msg: String): Unit = {}
    def warn(msg: String): Unit = {}
    def info(msg: String): Unit = {}
    def debug(msg: String): Unit = {}
  }

  case object Console extends Log {
    def error(msg: String) = info(msg)
    def warn(msg: String) = info(msg)
    def info(msg: String) = log synchronized {
      println(msg)
    }
    def debug(msg: String): Unit = {
      if (currentContext(Key.verbose)) log synchronized {
        println(msg)
      }
    }
  }

  class JLine extends Log {
    private val terminal = TerminalBuilder.builder()
      .system(true)
      .dumb(false)
      .build()
    private val display = new Display(terminal, true)
    private var overallStart = System.currentTimeMillis()
    private var overallPercent = 0.0
    private var overallBenchmark = new AttributedString(" ")
    private var currentStart = System.currentTimeMillis()
    private var currentPercent = 0.0
    private var currentInput = new AttributedString(" ")
    private var lastMessage = new AttributedString(" ")
    private var forkIndex = 0
    private var forkTotal = 0
    private var forkCommand = " "
    private var timerEnabled = false
    private val timer = new Timer("scalameter-jline-refresher", true)

    timer.schedule(new TimerTask {
      override def run(): Unit = JLine.this.synchronized {
        if (timerEnabled) redraw()
      }
    }, 0, 1000)
    display.resize(terminal.getBufferSize.getColumns, terminal.getBufferSize.getRows)

    Runtime.getRuntime.addShutdownHook(new Thread {
      override def run(): Unit = JLine.this.synchronized {
        timerEnabled = false
        clear()
      }
    })

    private def progressLine(percent: Double): AttributedString = {
      val totalCount = 24
      val fullCount = (percent / 100.0 * totalCount).toInt
      val ldelim = "\u25C1"
      val dot = "\u2024"
      val square = "\u25fc"
      val rdelim = "\u25b7"
      val content = ldelim + square * fullCount + dot * (totalCount - fullCount) + rdelim
      val style = new AttributedStyle().foreground(AttributedStyle.GREEN)
      new AttributedString(content, style)
    }

    private def outputLines: ArrayList[AttributedString] = {
      import AttributedString._
      val now = System.currentTimeMillis()
      val titleStyle = new AttributedStyle().foreground(AttributedStyle.BLUE).bold()
      val lines = new ArrayList[AttributedString]
      val scope = join(
        fromAnsi(""),
        new AttributedString("Scope:   ", titleStyle),
        overallBenchmark
      )
      val overallProgress = join(
        fromAnsi(""),
        new AttributedString("Overall: ", titleStyle),
        fromAnsi(DurationFormatUtils.formatDuration(now - overallStart, "HH:mm:ss")),
        fromAnsi(" "),
        progressLine(overallPercent),
        fromAnsi(" "),
        fromAnsi(s"Fork $forkIndex/$forkTotal "),
        fromAnsi("\ud83e\udc7a " + forkCommand)
      )
      val currentProgress = join(
        fromAnsi(""),
        new AttributedString("Current: ", titleStyle),
        fromAnsi(DurationFormatUtils.formatDuration(now - currentStart, "HH:mm:ss")),
        fromAnsi(" "),
        progressLine(currentPercent),
        fromAnsi(" Input "),
        currentInput
      )
      val message = lastMessage
      lines.add(scope)
      lines.add(overallProgress)
      lines.add(currentProgress)
      lines.add(message)
      lines
    }

    private def redraw(): Unit = {
      clear()
      val lines = outputLines
      display.update(lines, terminal.getSize.cursorPos(0, 0), true)
    }

    override def clear(): Unit = {
      val lines = new ArrayList[AttributedString]
      lines.add(AttributedString.fromAnsi(""))
      lines.add(AttributedString.fromAnsi(""))
      lines.add(AttributedString.fromAnsi(""))
      lines.add(AttributedString.fromAnsi(""))
      display.update(lines, terminal.getSize.cursorPos(0, 0), true)
    }

    override def overallBegin() = this.synchronized {
      overallStart = System.currentTimeMillis()
      redraw()
    }

    override def overallProgress(percent: Double): Unit = this.synchronized {
      overallPercent = math.max(0.0, math.min(100.0, percent))
      redraw()
    }

    override def overallBenchmark(benchmark: String): Unit = this.synchronized {
      overallBenchmark = AttributedString.fromAnsi(benchmark)
      redraw()
    }

    override def currentBegin() = this.synchronized {
      currentStart = System.currentTimeMillis()
      redraw()
    }

    override def currentForkIndex(n: Int): Unit = this.synchronized {
      forkIndex = n
      redraw()
    }

    override def currentTotalForks(n: Int): Unit = this.synchronized {
      forkTotal = n
      redraw()
    }

    override def currentForkCommand(cmd: String): Unit = this.synchronized {
      forkCommand = cmd
      redraw()
    }

    override def currentProgress(percent: Double): Unit = this.synchronized {
      currentPercent = math.max(0.0, math.min(100.0, percent))
      redraw()
    }

    override def currentInput(input: String): Unit = this.synchronized {
      currentInput = AttributedString.fromAnsi(input)
      redraw()
    }

    override def timer(enable: Boolean): Unit = this.synchronized {
      timerEnabled = enable
    }

    def error(msg: String) = this.synchronized {
      val style = new AttributedStyle().foreground(AttributedStyle.RED)
      lastMessage = new AttributedString(msg, style)
      redraw()
    }

    def warn(msg: String) = this.synchronized {
      val style = new AttributedStyle().foreground(AttributedStyle.YELLOW)
      lastMessage = new AttributedString(msg, style)
      redraw()
    }

    def info(msg: String) = this.synchronized {
      val style = new AttributedStyle().foreground(AttributedStyle.GREEN)
      lastMessage = new AttributedString(msg, style)
      redraw()
    }

    def debug(msg: String): Unit = this.synchronized {
      val style = new AttributedStyle().foreground(AttributedStyle.WHITE)
      lastMessage = new AttributedString(msg, style)
      redraw()
    }
  }

  case class Composite(logs: Log*) extends Log {
    def error(msg: String) = for (l <- logs) l.error(msg)
    def warn(msg: String) = for (l <- logs) l.warn(msg)
    def info(msg: String) = for (l <- logs) l.info(msg)
    def debug(msg: String) = for (l <- logs) l.debug(msg)
  }

  val default = {
    try {
      new JLine
    } catch {
      case t: Throwable =>
        Console
    }
  }

  def main(args: Array[String]): Unit = {
    log.info("Hi.")
    log.timer(true)
    Thread.sleep(2000)
    log.overallBenchmark("DefaultBenchmark")
    log.overallProgress(10)
    log.error("Hm, getting suspicious.")
    Thread.sleep(1000)
    log.overallProgress(20)
    log.warn("Seems to work...")
    Thread.sleep(1000)
    log.overallProgress(30)
    log.debug("Lots of info...")
    Thread.sleep(1000)
    log.overallProgress(40)
    log.debug("Still info...")
    log.overallBenchmark("NumericBenchmark")
    log.currentBegin()
    log.currentForkIndex(0)
    log.currentTotalForks(2)
    log.currentForkCommand("java -cp ...")
    log.currentInput("10")
    Thread.sleep(300)
    log.currentInput("20")
    Thread.sleep(500)
    log.currentInput("40")
    Thread.sleep(1000)
    log.overallProgress(50)
    log.debug("Working...")
    log.currentForkIndex(1)
    Thread.sleep(1000)
    log.overallProgress(60)
    log.overallBenchmark("ArithmeticBenchmark")
    log.currentBegin()
    log.currentInput("10")
    Thread.sleep(500)
    log.warn("What...")
    Thread.sleep(500)
    log.error("Hey!")
    log.currentInput("20")
    Thread.sleep(1000)
    log.debug("Nah, all good. Working...")
    log.currentProgress(10)
    log.currentInput("30")
    log.overallProgress(80)
    Thread.sleep(1000)
    log.currentProgress(20)
    log.currentInput("40")
    Thread.sleep(1000)
    log.timer(false)
  }
}
