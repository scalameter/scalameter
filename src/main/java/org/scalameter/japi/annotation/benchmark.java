package org.scalameter.japi.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/** Annotates benchmark snippet.
 *
 *  Its value sets the scope name of the snippet.
 *
 *  The following declaration:
 *
 *  {{{
 *    @benchmark("arrays.forloops")
 *    public void snippetMethod(int[] xs) {
 *      // ...
 *    }
 *  }}}
 *
 *  declares a benchmark snippet, equivalent to:
 *
 *  {{{
 *  performance of "arrays" in {
 *    measure method "forloops" in {
 *      // ...
 *    }
 *  }
 *  }}}
 *
 *  Note that method annotated with `@benchmark` should be public
 *  and have a single argument of type T, such that it corresponds to the
 *  type of the generator declared in the @gen annotation.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface benchmark {
  String value();
}
