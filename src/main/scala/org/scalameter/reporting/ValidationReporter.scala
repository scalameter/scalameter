package org.scalameter
package reporting



import org.scalameter.utils.Tree



/** Validates every measurement using the `reports.validation.predicate` key.
 *
 *  This key stores the predicate function that does the validation.
 */
case class ValidationReporter[T]() extends Reporter[T] {

  def report(result: CurveData[T], persistor: Persistor): Unit = {}

  def report(results: Tree[CurveData[T]], persistor: Persistor) = {
    var totalValidations = 0
    var totalSuccesses = 0
    val oks = for {
      (context, curves) <- results.scopes
      if curves.nonEmpty
    } {
      for (curve <- curves) curve.context.get(Key.reports.validation.predicate) match {
        case None => // no need to validate
        case Some(f) =>
          for (measurement <- curve.measurements) {
            totalValidations += 1
            if (f.asInstanceOf[T => Boolean](measurement.value)) {
              totalSuccesses += 1
              events.emit(Event(context.scope, "Success", Events.Success, null))
            } else {
              events.emit(Event(context.scope, "Failure", Events.Failure, null))
            }
          }
      }
    }

    val isSuccess = totalValidations == totalSuccesses
    val color = if (isSuccess) ansi.green else ansi.red
    log(s"${color}Summary: $totalSuccesses/$totalValidations passed.${ansi.reset}")

    isSuccess
  }

}
