package org.scalameter.javaApi

import org.scalameter.Gen
import java.lang.Integer
import scala.collection.JavaConverters._
import java.util.Arrays
import java.util.Collection
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap

abstract class JavaGenerator[T] extends Serializable{
  def get(): Gen[_]
  def map[T, S](m: MapFunction[T, S]): JavaGenerator[S] = {
    def f: T => S = (t: T) => { m.map(t) }
    return new JavaGen(get.asInstanceOf[Gen[T]].map(f))
  }
  def flatmap[T, S](fm: FlatmapFunction[T, S]): JavaGenerator[S] = {
    def f: T => Gen[S] = (t: T) => { fm.flatmap(t).get.asInstanceOf[Gen[S]] }
    return new JavaGen(get.asInstanceOf[Gen[T]].flatMap(f))
  }
  def zip[S](g: JavaGenerator[S]): JavaGenerator[scala.Tuple2[T, S]] = {
    return new TupledJavaGen(get.asInstanceOf[Gen[T]].zip(g.get.asInstanceOf[Gen[S]]))
  }
}

class VoidGen(axisName: String) extends JavaGenerator[Void] {
  def get = Gen.unit(axisName)
}

class SingleGen[T](axisName: String, v: T) extends JavaGenerator[T] {
  def get = Gen.single(axisName)(v)
}

class RangeGen(axisName: String, from: Int, upto: Int, hop: Int) extends JavaGenerator[Integer] {
  def get = Gen.range(axisName)(from, upto, hop)
}

class EnumerationGen[T](axisName: String, xs: Array[T]) extends JavaGenerator[T] {
  def get = {
    var ys: List[T] = List()
    for (x <- xs) {
      ys = x :: ys
    }
    Gen.enumeration(axisName)(ys: _*)
  }
}

class ExponentialGen(axisName: String, from: Int, until: Int, factor: Int) extends JavaGenerator[java.lang.Integer] {
  def get = Gen.exponential(axisName)(from, until, factor)
}

class TupledGen[P, Q](p: JavaGenerator[P], q: JavaGenerator[Q]) extends JavaGenerator[Tuple2[P, Q]] {
  def get: Gen[(P, Q)] = Gen.tupled(p.get.asInstanceOf[Gen[P]], q.get.asInstanceOf[Gen[Q]])
}

class Tupled3Gen[P, Q, R](p: JavaGenerator[P], q: JavaGenerator[Q], r: JavaGenerator[R]) extends JavaGenerator[Tuple3[P, Q, R]] {
  def get: Gen[(P, Q, R)] = Gen.tupled(p.get.asInstanceOf[Gen[P]], q.get.asInstanceOf[Gen[Q]], r.get.asInstanceOf[Gen[R]])
}
class Tupled4Gen[P, Q, R, S](p: JavaGenerator[P], q: JavaGenerator[Q], r: JavaGenerator[R], s: JavaGenerator[S]) extends JavaGenerator[Tuple4[P, Q, R, S]] {
  def get: Gen[(P, Q, R, S)] = Gen.tupled(p.get.asInstanceOf[Gen[P]], q.get.asInstanceOf[Gen[Q]], r.get.asInstanceOf[Gen[R]], s.get.asInstanceOf[Gen[S]])
}
class JavaGen[T](g: Gen[T]) extends JavaGenerator[T] with Serializable{
  def get = g
}

class TupledJavaGen[P, Q](g: Gen[(P, Q)]) extends JavaGenerator[scala.Tuple2[P, Q]] {
  def get = g
}

class Collections(sizes: JavaGenerator[Integer]) {
  def s = new C(sizes)
  def lists: JavaGen[java.util.LinkedList[Integer]] = new JavaGen(s.javaLists)

  def arrays: JavaGen[Array[Int]] = new JavaGen(s.arrays.asInstanceOf[Gen[Array[Int]]])

  def vectors: JavaGen[java.util.Vector[Integer]] = new JavaGen(s.javavectors)

  def hashtablemaps: JavaGenerator[java.util.HashMap[Integer, Integer]] = new JavaGen(s.javahashtablemaps)

  def linkedhashtablemaps: JavaGenerator[java.util.LinkedHashMap[Integer, Integer]] = new JavaGen(s.javalinkedhashtablemaps)

  def treemap: JavaGenerator[java.util.TreeMap[Integer, Integer]] = new JavaGen(s.javaredblackmaps)

  def hashtablesets: JavaGenerator[java.util.HashSet[Integer]] = new JavaGen(s.javahashtablesets)

  def linkedhashtablesets: JavaGenerator[java.util.LinkedHashSet[Integer]] = new JavaGen(s.javalinkedhashtablesets)

  def avlsets: JavaGenerator[java.util.TreeSet[Integer]] = new JavaGen(s.javaavlsets)

  class C(s: JavaGenerator[Integer]) extends Gen.Collections{
    def sizes = s.get.asInstanceOf[Gen[Int]]

    def javaLists = for {
      size <- sizes
    } yield {
      var l = new java.util.LinkedList[Integer]
      for (x <- 0 until size) l.add(x)
      l
    }

    def javavectors = for {
      size <- sizes
    } yield {
      var v = new java.util.Vector[Integer](size)
      for (x <- 0 until size) v.add(x)
      v
    }
    
     
    def javahashtablemaps = for {
      size <- sizes
    } yield {
      val hm = new java.util.HashMap[Integer, Integer]
      for (x <- 0 until size) hm.put(x, x)
      hm
    }

     def javalinkedhashtablemaps = for {
      size <- sizes
    } yield {
      val hm = new java.util.LinkedHashMap[Integer, Integer]()
      for (x <- 0 until size) hm.put(x, x)
      hm
    }
    
    def javaredblackmaps = for {
      size <- sizes
    } yield {
      var am = new java.util.TreeMap[Integer, Integer]()
      for (x <- 0 until size) am.put(x, x)
      am
    }
    
      def javahashtablesets = for {
      size <- sizes
    } yield {
      val hs = new java.util.HashSet[Integer]()
      for (x <- 0 until size) hs.add(x)
      hs
    }
    
    def javalinkedhashtablesets = for {
      size <- sizes
    } yield {
      val hs = new java.util.LinkedHashSet[Integer]()
      for (x <- 0 until size) hs.add(x)
      hs
    }
      
    def javaavlsets = for {
      size <- sizes
    } yield {
      val as = new java.util.TreeSet[Integer]()
      for (x <- 0 until size) as.add(x)
      as
    }
  }
}