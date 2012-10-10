package org.collperf
package persistance



import java.util.Date
import java.io._
import collection._



case class SerializationPersistor extends Persistor {
  private def loadHistory(dir: String, scope: String): History = {
    val file = new File(s"$dir${File.separator}$scope")
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
    val fos = new FileOutputStream(s"$dir${File.separator}$scope")
    val oos = new ObjectOutputStream(fos)
    try {
      oos.writeObject(h)
    } finally {
      oos.close()
    }
  }

  def load(context: Context): History = {
    val scope = context.scope
    val resultdir = context.goe(Key.resultDir, "tmp")
    loadHistory(resultdir, scope)
  }

  def save(context: Context, result: Seq[CurveData]) {
    val scope = context.scope
    val resultdir = context.goe(Key.resultDir, "tmp")
    val history = loadHistory(resultdir, scope)
    val newhistory = History(history.results :+ (new Date, context, result))
    saveHistory(resultdir, scope, newhistory)
  }
}

