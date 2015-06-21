package org.scalameter.persistence

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper


package object json {
  /** Jackson ObjectMapper that maps concrete class instances to JSON, and vice versa.
   *
   *  It supports out of a box primitives, classes and Scala-specific datatypes thanks to `jackson-module-scala`.
   *
   *  Note that [[scala.collection.immutable.Map]] with [[org.scalameter.Key]] or [[org.scalameter.Parameter]] as a key
   *  and [[scala.Any]] as a value is serialized using [[org.scalameter.picklers.Pickler]].
   *  It means that every map value is serialized as Base64 encoded byte array.
   *
   *  Note that supporting inheritance needs annotating supertype with
   *  [[com.fasterxml.jackson.annotation.JsonTypeInfo]] and [[com.fasterxml.jackson.annotation.JsonSubTypes]].
   *  If it is not achievable, Jackson Mix-in Annotation can be used.
   *  {{{
   *    import com.fasterxml.jackson.annotation.{JsonTypeInfo, JsonSubTypes}
   *    import org.scalameter.persistence.json._
   *
   *    @JsonTypeInfo(use=Id.CLASS)
   *    @JsonSubTypes(Array(classOf[Apple], classOf[Orange])
   *    abstract class FruitMixin
   *
   *    jsonMapper.addMixin[Fruit, FruitMixin]
   *  }}}
   */
  private[persistence] lazy val jsonMapper = {
    val mapper = new ObjectMapper with ScalaObjectMapper
    mapper.registerModules(DefaultScalaModule, ScalaMeterModule)
    mapper
  }
}
