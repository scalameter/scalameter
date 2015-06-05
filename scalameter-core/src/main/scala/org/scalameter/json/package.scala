package org.scalameter

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper


package object json {
  lazy val mapper = {
    val mapper = new ObjectMapper with ScalaObjectMapper
    mapper.registerModules(DefaultScalaModule, ScalaMeterModule)
    mapper
  }
}
