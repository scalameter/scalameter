package org.scalameter


trait Log {
  def error(msg: String): Unit
  def warn(msg: String): Unit
  def info(msg: String): Unit
  def debug(msg: String): Unit
  def trace(t: Throwable): Unit

  def verbose(msg: =>Any) = debug(msg.toString)
  def apply(msg: =>Any) = info(msg.toString)
}

object Log {

  case object None extends Log {
    def error(msg: String) {}
    def warn(msg: String) {}
    def info(msg: String) {}
    def debug(msg: String) {}
    def trace(t: Throwable) {}
  }

  case object Console extends Log {
    def error(msg: String) = info(msg)
    def warn(msg: String) = info(msg)
    def trace(t: Throwable) = info(t.getMessage)
    def info(msg: String) = log synchronized {
      println(msg)
    }
    def debug(msg: String) {
      if (currentContext(Key.verbose)) log synchronized {
        println(msg)
      }
    }
  }

  case class Composite(logs: Log*) extends Log {
    def error(msg: String) = for (l <- logs) l.error(msg)
    def warn(msg: String) = for (l <- logs) l.warn(msg)
    def trace(t: Throwable) = for (l <- logs) l.trace(t)
    def info(msg: String) = for (l <- logs) l.info(msg)
    def debug(msg: String) = for (l <- logs) l.debug(msg)
  }

}

