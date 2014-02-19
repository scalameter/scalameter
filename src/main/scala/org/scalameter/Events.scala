package org.scalameter


case class Event(testName: String, description: String, result: Events.Result, throwable: Throwable)

trait Events {
  def emit(e: Event): Unit
}

object Events {
  sealed trait Result
  case object Success extends Result
  case object Failure extends Result
  case object Error extends Result
  case object Skipped extends Result

  case object None extends Events {
    def emit(e: Event) {}
  }
}
