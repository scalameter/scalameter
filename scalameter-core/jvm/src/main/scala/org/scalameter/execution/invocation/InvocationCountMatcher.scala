package org.scalameter.execution.invocation

import java.lang.reflect.Method
import java.util.regex.Pattern
import org.objectweb.asm.Type
import scala.annotation.tailrec
import scala.util.matching.Regex


/** Object that matches the methods whose invocations should be counted.
 *
 *  @param classMatcher matches full class name given in internal format ('/' instead of a '.' as a separator)
 *  @param methodMatcher matches a specific method
 */
case class InvocationCountMatcher(classMatcher: InvocationCountMatcher.ClassMatcher, methodMatcher: InvocationCountMatcher.MethodMatcher) {
  def classMatches(className: String): Boolean = classMatcher.matches(className)

  def methodMatches(methodName: String, methodDescriptor: String): Boolean = methodMatcher.matches(methodName, methodDescriptor)
}

object InvocationCountMatcher {
  /** Mixin used for selecting classes whose methods will be checked against [[MethodMatcher]] and if matched,
   *  counted by a method invocation counting measurer.
   */
  sealed trait ClassMatcher {
    /** Matches class name given in a standard format ('.' as a package separator).
     *
     *  @param className class name that is matched
     */
    def matches(className: String): Boolean
  }

  object ClassMatcher {
    /** Matches class with a class name given as a string. */
    case class ClassName(clazz: String) extends ClassMatcher {
      def matches(className: String): Boolean = className == clazz
    }

    object ClassName {
      def apply(clazz: Class[_]) = new ClassName(clazz.getName)
    }

    /** Matches class with a regex.
      *
      *  Note that package separation in `regex` should be done by escaping '.'
      *  {{{
      *    val pattern = "java\\.lang\\.String".r.pattern
      *  }}}
      */
    case class Regex(regex: Pattern) extends ClassMatcher {
      def matches(className: String): Boolean = regex.matcher(className).matches()
    }

    /** Matches class that is a descendant of the [[baseClazz]].
     *
     *  @param baseClazz plain class or a mixin that the given class name should be a descendant.
     *  @param direct when true checks only if class is a direct child of the [[baseClazz]].
     *  @param withSelf when true class is also matched if it is the [[baseClazz]].
     */
    case class Descendants(baseClazz: String, direct: Boolean, withSelf: Boolean) extends ClassMatcher {
      def matches(className: String): Boolean = {
        /** Gets ancestors of a given class.
         *
         *  Note that it returns both superclass and interfaces.
         */
        def getAncestors(of: Class[_]): Set[Class[_]] = {
          val interfaces = of.getInterfaces.toSet
          val parent = of.getSuperclass.asInstanceOf[Class[_]]

          if (parent != null) interfaces + parent else interfaces
        }

        /** Checks if any of ancestors is a [[baseClazz]].
         *
         *  Note that search is done in a breadth-first search manner.
         */
        @tailrec
        def matches(parents: Iterator[Class[_]], visited: Set[Class[_]]): Boolean = {
          val ancestors: Map[String, Class[_]] = (for {
            parent <- parents
            ancestor <- getAncestors(parent) if !visited.contains(ancestor)
          } yield ancestor.getName -> ancestor).toMap

          if (ancestors.contains(baseClazz)) true
          else if (ancestors.isEmpty) false
          else matches(ancestors.valuesIterator, visited ++ ancestors.valuesIterator)
        }

        val parents: Map[String, Class[_]] = try {
          getAncestors(Class.forName(className)).iterator.map(clazz => clazz.getName -> clazz).toMap
        } catch {
          case ex: Throwable => Map.empty
        }

        if (!withSelf && className == baseClazz) false // early cut
        else if (withSelf && className == baseClazz) true
        else if (parents.contains(baseClazz)) true
        else if (!direct) matches(parents.valuesIterator, Set.empty)
        else false
      }
    }

    object Descendants {
      def apply(clazz: Class[_], direct: Boolean, withSelf: Boolean) = new Descendants(clazz.getName, direct, withSelf)
    }
  }
  
  /** Mixin used for selecting methods whose invocations should be counted by a method invocation counting measurer.
   *
   *  Note that selecting classes for which every method will be checked against `matches` method is done by [[ClassMatcher]].
   */
  sealed trait MethodMatcher {
    /**  Matches method given in a internal jvm format.
      *
      *  Note that only [[MethodMatcher.Full]] matcher actually matches `methodDescriptor`.
      *
      *  @param methodName method name that is matched
      *  @param methodDescriptor method descriptor in format specified by a JVM spec
      *
      *  @see https://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.3.3
      */
    def matches(methodName: String, methodDescriptor: String): Boolean
  }
  
  object MethodMatcher {
    /** Matches method with a method name given as a string. */
    case class MethodName(method: String) extends MethodMatcher {
      def matches(methodName: String, methodDescriptor: String): Boolean = methodName == method
    }

    /** Matches class allocations, which are basically call to special `<init>` method. */
    case object Allocation extends MethodMatcher {
      def matches(methodName: String, methodDescriptor: String): Boolean = methodName == "<init>"
    }

    /** Matches method with a regex. */
    case class Regex(regex: Pattern) extends MethodMatcher {
      def matches(methodName: String, methodDescriptor: String): Boolean = regex.matcher(methodName).matches()
    }

    /**  Matches method with a name and its descriptor.
      *
      *  That means that method name, method arguments and method return type are matched.
      */
    case class Full(name: String, descriptor: String) extends MethodMatcher {
      def matches(methodName: String, methodDescriptor: String): Boolean =
        methodName == name && methodDescriptor == descriptor
    }

    object Full {
      def apply(method: Method): Full = new Full(name = method.getName, descriptor = Type.getMethodDescriptor(method))
    }
  }

  /** Matches allocations of a class. */
  def allocations(clazz: Class[_]) = 
    new InvocationCountMatcher(ClassMatcher.ClassName(clazz), MethodMatcher.Allocation)

  /** Matches method name in a class. */
  def forName(className: String, methodName: String) =
    new InvocationCountMatcher(ClassMatcher.ClassName(className), MethodMatcher.MethodName(methodName))
  
  /** Matches method with a [[java.lang.reflect.Method]] in a class. */
  def forClass(clazz: Class[_], method: Method) =
    new InvocationCountMatcher(ClassMatcher.ClassName(clazz), MethodMatcher.Full(method))

  /** Matches method name with a regex in classes matched by a regex. */
  def forRegex(classRegex: Regex, methodRegex: Regex) =
    new InvocationCountMatcher(ClassMatcher.Regex(classRegex.pattern), MethodMatcher.Regex(methodRegex.pattern))
}
