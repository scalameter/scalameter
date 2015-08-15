package org.scalameter.execution.invocation.instrumentation

import org.objectweb.asm.{Type, ClassVisitor, MethodVisitor, Opcodes}
import org.scalameter.execution.invocation.InvocationCountMatcher
import scala.collection.mutable


/** Walks over methods in a class and instruments ones that are matched by [[org.scalameter.execution.invocation.InvocationCountMatcher]].
 *
 * @param cv [[org.objectweb.asm.ClassVisitor]] that actually visits classes and methods
 * @param pattern selects methods that needs to be instrumented
 * @param counterClass class of counting class
 * @param counterMethod method name of counting class
 * @param initialIndex counting table start index
 */
private[instrumentation] class MethodInvocationCounterVisitor(cv: ClassVisitor, pattern: InvocationCountMatcher,
                                                              counterClass: String, counterMethod: String,
                                                              initialIndex: Int)
  extends ClassVisitor(Opcodes.ASM5, cv) {
  private val rawMethods: mutable.ArrayBuffer[MethodSignature] = mutable.ArrayBuffer.empty[MethodSignature]
  private var className: String = _

  override def visit(version: Int, access: Int, name: String, signature: String, superName: String, interfaces: Array[String]): Unit = {
    className = name.replace('/', '.')
    super.visit(version, access, name, signature, superName, interfaces)
  }

  /** Inserts [[counterClass]].[[counterMethod]]([[initialIndex]]) call at the beginning of a method that matches a [[pattern]]. */
  override def visitMethod(access: Int, name: String, desc: String, signature: String, exceptions: Array[String]): MethodVisitor = {
    require(className != null)

    val mv = cv.visitMethod(access, name, desc, signature, exceptions)
    if (className != counterClass && pattern.methodMatches(name, desc) && (access & Opcodes.ACC_ABSTRACT) == 0) {
      mv.visitLdcInsn(initialIndex + rawMethods.length)
      mv.visitMethodInsn(Opcodes.INVOKESTATIC, counterClass, counterMethod, "(I)V", false)
      val method = MethodSignature(className = className, methodName = name, Type.getArgumentTypes(desc).map(_.getClassName): _*)
      rawMethods += method
    }

    mv
  }

  /** Returns current index after instrumenting all matched methods. */
  def currentIndex: Int = initialIndex + methods.length

  /** Returns all matched methods. */
  def methods: Iterator[MethodSignature] = rawMethods.iterator
}
