package org.scalameter.execution.invocation.instrumentation;

import java.util.concurrent.atomic.AtomicLongArray;


/** Internal API
 *
 *  Stores method invocations in an AtomicLongArray.
 *
 *  Note that this class is a Java class because we want method [[methodCalled]] to be static,
 *  which makes the bytecode instrumentation easier and more compact.
 */
public class MethodInvocationCounter {
  private enum State {
      UNINITIALIZED, INITIALIZED, STARTED, STOPPED
  }
  private static volatile State state = State.UNINITIALIZED;
  private static AtomicLongArray rawCounts;

  /** Initializes count array.
   *
   *  @param numMethods length of an array
   */
  public static synchronized void setup(int numMethods) {
      rawCounts = new AtomicLongArray(numMethods);
      state = State.INITIALIZED;
  }

  /** Starts measuring of method invocations.
   *
   *  Note that, it should be called right before counting method invocations in specific thunk of code
   *  in order to avoid catching of any additional counts that are not a part of the measurement.
   */
  public static synchronized void start() {
    if (state == State.INITIALIZED || state == State.STOPPED) {
      state = State.STARTED;
    }
  }

  /** Stops measuring of method invocations.
   *
   *  Note that it should be called right after counting method invocations in specific thunk of code
   *  in order to avoid catching of any additional counts that are not a part of the measurement.
   */
  public static synchronized void stop() {
    if (state == State.STARTED) {
      state = State.STOPPED;
    }
  }

  /** Increments method invocation count at a given index.
   *
   *  Note that this method shouldn't be called manually. It is called by an instrumented code itself.
   */
  public static void methodCalled(int index) {
    if (state == State.STARTED) {
      rawCounts.incrementAndGet(index);
    }
  }

  /** Returns count array.
   */
  public static long[] counts() {
    if (state == State.STOPPED) {
      long[] counts = new long[rawCounts.length()];
      for (int i = 0; i < rawCounts.length(); i++) {
        counts[i] = rawCounts.get(i);
      }
      return counts;
    } else {
      throw new RuntimeException("counts() can be called only after call to the stop() method");
    }
  }
}
