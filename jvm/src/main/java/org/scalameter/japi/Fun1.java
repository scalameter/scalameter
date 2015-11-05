package org.scalameter.japi;

import java.io.Serializable;


/** JGen function interface for `map` combinator.
 *
 *  Note that it can be used from Java code using both lambda syntax (Java 8)
 *  and plain new instance creation (Java <= 8).
 *
 *  {{{
 *    JGen.range("x", 100, 1000, 100).map(x -> new int[x]) // valid for the Java 8
 *
 *    JGen.range("x", 100, 1000, 100).map(new Fun1<Integer, int[]>() {
 *      public int[] apply(Integer v) {
 *        return new int[v];
 *      }
 *    })
 *  }}}
 */
public interface Fun1<T, R> extends Serializable {
  R apply(T v);
}
