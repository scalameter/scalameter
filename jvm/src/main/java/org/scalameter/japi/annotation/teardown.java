package org.scalameter.japi.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/** Optional annotation that points to a teardown method for the benchmark snippet.
 *
 *  Its value should point to a public one-arg method, whose argument's type should be
 *  the same as type of benchmark method argument.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface teardown {
  String value();
}
