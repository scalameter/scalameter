package org.scalameter.japi.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/** Optional annotation that points to a
 *  `setupBeforeAll` method -- the method in the
 *  same class that gets invoked before all the benchmarks runs.
 *
 *  Its value should point to a public no-arg method.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface setupBeforeAll {
  String value();
}
