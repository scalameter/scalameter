package org.scalameter.execution.invocation.instrumentation


/** Contains method signature in form of className.methodName(methodArgs...)
 *  where methodArgs are written as Java types.
 *
 *  Note that it does not contain return type, because we cannot differentiate methods based on their return types.
 *  {{{
 *    // method signature for a method `(range: scala.collection.immutable.Range).by(idx: Int)`
 *    MethodSignature("scala.collection.immutable.Range", "by", "int").toString == "scala.collection.immutable.Range.by(int)"
 *  }}}
 */
case class MethodSignature(className: String, methodName: String, methodArgs: String*)  {
  override def toString: String = s"$className.$methodName(${methodArgs.mkString(", ")})"
}
