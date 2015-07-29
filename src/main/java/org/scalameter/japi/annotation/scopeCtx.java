package org.scalameter.japi.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/** Allows setting custom context for a scope.
 *
 *  [[scope()]] should point to a scope.
 *  [[context()]] should point to either a public field
 *  or a public no-arg method name with return type `Context`.
 *
 *  Note that it should be only used with combination with @scopes
 *  {{{
 *    @scopes({
 *      // sets context for the scope "arrays.forloops" pointed by a `ctxVariable`
 *      @scopeCtx(scope = "arrays.forloops", ctx = "ctxVariable")
 *    )}
 *    class ArrayBenchmark extends JBench.Micro {
 *      public final ctxVariable: Context = new ContextBuilder().build();
 *
 *      @benchmark("arrays.forloops")
 *      public int sum(int[] array) {
 *        // ...
 *      }
 *    }
 *  }}}
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.ANNOTATION_TYPE})
public @interface scopeCtx {
  String scope();
  String context();
}
