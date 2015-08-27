package org.scalameter






case class Setup[T](
  context: Context,
  gen: Gen[T],
  setupbeforeall: Option[() => Unit],
  teardownafterall: Option[() => Unit],
  setup: Option[T => Any],
  teardown: Option[T => Any],
  customwarmup: Option[() => Any],
  snippet: T => Any
) {
  def setupBeforeAll =
    if (setupbeforeall.isEmpty) { () => } else { () => setupbeforeall.get.apply() }
  def teardownAfterAll =
    if (teardownafterall.isEmpty) { () => } else { () => teardownafterall.get.apply() }
  def setupFor(v: T) =
    if (setup.isEmpty) { () => } else { () => setup.get(v) }
  def teardownFor(v: T) =
    if (teardown.isEmpty) { () => } else { () => teardown.get(v) }
  def setupFor() =
    if (setup.isEmpty) { v: T => } else { v: T => setup.get(v) }
  def teardownFor() =
    if (teardown.isEmpty) { v: T => } else { v: T => teardown.get(v) }
  def regenerateFor(params: Parameters): () => T =
    () => gen.generate(params)
}
