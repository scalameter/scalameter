package org.scalameter.japi.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/** Points to the generator used for the benchmark snippet.
 *
 *  Its value should point to either a public field
 *  or a public no-arg method name method name with return type `Gen[T]`.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface gen {
  String value();
}
