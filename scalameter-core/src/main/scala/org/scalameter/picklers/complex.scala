package org.scalameter.picklers

import java.io.File
import org.scalameter.utils.ClassPath


object ClassPathPickler extends Pickler[ClassPath] {
  override def pickle(x: ClassPath): Array[Byte] = StringListPickler.pickle(x.paths.map(_.getAbsolutePath)(collection.breakOut))

  override def unpickle(a: Array[Byte], from: Int): (ClassPath, Int) = {
    val (cl, pos) = StringListPickler.unpickle(a, from)
    (ClassPath(cl.map(new File(_))), pos)
  }
}
