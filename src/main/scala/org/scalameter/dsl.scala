package org.scalameter


import org.scalameter.picklers.Pickler
import scala.reflect.ClassTag



abstract class DSL[U] extends BasePerformanceTest[U] {

  import BasePerformanceTest._

  case object performance {
    def of(modulename: String) = Scope(modulename, setupzipper.value.current.context)
  }

  case object measure {
    def method(methodname: String) = Scope(methodname, setupzipper.value.current.context)
  }

  def using[T](gen: Gen[T]) = Using(Setup(setupzipper.value.current.context +
    (Key.dsl.curve -> freshCurveName()), gen, None, None, None, None, None, null))

  def include[T <: BasePerformanceTest[_]: ClassTag]: Unit = {
    val cls = implicitly[ClassTag[T]].runtimeClass
    if (isModule) {
      log.error(s"Can only use `include` with class benchmarks -- make ${cls.getName} a class.")
      events.emit(Event(
        cls.getName, s"The `include` can only be used with benchmarks in classes -- make ${cls.getName} a class.",
        Events.Error, new Exception("Cannot instantiate singleton object.")))
    } else cls.newInstance.asInstanceOf[DSL[_]]
  }

}


object DSL {

}





