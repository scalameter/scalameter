package org.scalameter.picklers



import java.io.File
import org.scalameter.utils.ClassPath



object ClassPathPickler extends Pickler[ClassPath] {
  override def pickle(x: ClassPath): Array[Byte] =
    StringListPickler.pickle(x.paths.iterator.map(_.getAbsolutePath).toList)

  override def unpickle(a: Array[Byte], from: Int): (ClassPath, Int) = {
    val (cl, pos) = StringListPickler.unpickle(a, from)
    (ClassPath(cl.map(new File(_))), pos)
  }
}

class TuplePickler[A, B](picklerA: Pickler[A], picklerB: Pickler[B]) extends Pickler[(A, B)] {
  override def pickle(x: (A, B)): Array[Byte] = {
    val builder = Array.newBuilder[Byte]
    builder ++= picklerA.pickle(x._1)
    builder ++= picklerB.pickle(x._2)
    builder.result()
  }

  override def unpickle(data: Array[Byte], from: Int): ((A, B), Int) = {
    val (a, fromA) = picklerA.unpickle(data, from)
    val (b, fromB) = picklerB.unpickle(data, fromA)
    ((a, b), fromB)
  }
}