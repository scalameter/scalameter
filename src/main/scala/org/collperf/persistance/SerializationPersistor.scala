package org.collperf
package persistance



import java.util.Date
import java.io._
import collection._
import Key.reporting._



case class SerializationPersistor(path: File) extends Persistor {

  def this(path: String) = this(new File(path))

  def this() = this(initialContext.goe(resultDir, ""))

  def sep = File.separator

  private def loadHistory(dir: String, scope: String): History = {
    val file = new File(s"$path$sep$scope")
    if (!file.exists || !file.isFile) History(Nil)
    else {
      val fis = new FileInputStream(file)
      val ois = new ObjectInputStream(fis)
      try {
        ois.readObject().asInstanceOf[History]
      } finally {
        ois.close()
      }
    }
  }

  private def saveHistory(dir: String, scope: String, h: History) {
    path.mkdir()
    val file = new File(s"$path$sep$scope")
    val fos = new FileOutputStream(file)
    val oos = new ObjectOutputStream(fos)
    try {
      oos.writeObject(h)
    } finally {
      oos.close()
    }
  }

  def load(context: Context): History = {
    val scope = context.scope
    val resultdir = context.goe(resultDir, "tmp")
    loadHistory(resultdir, scope)
  }

  def save(context: Context, h: History) {
    val scope = context.scope
    val resultdir = context.goe(resultDir, "tmp")
    saveHistory(resultdir, scope, h)
  }
}

