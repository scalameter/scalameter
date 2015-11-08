package org.scalameter.execution.invocation.instrumentation;



/** HACK: scalajs mock
 *  Internal API
 *
 *  Stores method invocations in an AtomicLongArray.
 *
 *  Note that this class is a Java class because we want method [[methodCalled]] to be static,
 *  which makes the bytecode instrumentation easier and more compact.
 */
object MethodInvocationCounter {
 
  /** Initializes count array.
   *
   *  @param numMethods length of an array
   */
  def setup(numMethods: Int) : Unit = ???

  /** Starts measuring of method invocations.
   *
   *  Note that, it should be called right before counting method invocations in specific thunk of code
   *  in order to avoid catching of any additional counts that are not a part of the measurement.
   */
  def start() : Unit = ???

  /** Stops measuring of method invocations.
   *
   *  Note that it should be called right after counting method invocations in specific thunk of code
   *  in order to avoid catching of any additional counts that are not a part of the measurement.
   */
  def stop() : Unit = ???

  /** Increments method invocation count at a given index.
   *
   *  Note that this method shouldn't be called manually. It is called by an instrumented code itself.
   */
  def methodCalled(index: Int) : Unit = ???

  /** Returns count array.
   */
  def counts() : Array[Long] = ???
  
}