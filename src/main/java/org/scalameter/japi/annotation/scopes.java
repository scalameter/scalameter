package org.scalameter.japi.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/** Allows setting custom contexts for scopes.
 *
 *  It should consist of @scopeCtx that each points to a single scope and context.
 *
 *  Note that it should be placed over a class
 *  that extends `JBench` and contains benchmark methods.
 *  {{{
 *    @scopes({
 *      // sets context for the scope "arrays.forloops" pointed by a `ctxVariable`
 *      @scopeCtx(scope = "arrays.forloops", ctx = "ctxVariable")
 *    )}
 *    class ArrayBenchmark extends JBench.Micro {
 *      // @...
 *      @benchmark("arrays.forloops")
 *      public int sum(int[] array) {
 *        // ...
 *      }
 *    }
 *  }}}
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface scopes {
  scopeCtx[] value();
}
