package org.scalameter.japi.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/** Optional annotation that points to the context of the annotated benchmarks snippet.
 *
 *  Its value should point to either a public field
 *  or a public no-arg method name with return type `Context`.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface ctx {
  String value();
}
