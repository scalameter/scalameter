package org.scalameter.execution

import java.io._

class Main

object Main {
  def main(args: Array[String]) {
    val tmpfile = new File(args(0))
    mainMethod(tmpfile)
  }

  def mainMethod(tmpfile: File) {
    try {
      val body = loadBody(tmpfile)
      val result = body()
      saveResult(tmpfile, result)
    } catch {
      case t: Throwable =>
        saveFailure(tmpfile, t)
    }
  }

  private def loadBody(file: File): () => Any = {
    val fis = new FileInputStream(file)
    val ois = new ObjectInputStream(fis)
    try {
      ois.readObject().asInstanceOf[() => Any]
    } finally {
      fis.close()
      ois.close()
    }
  }

  private def saveResult[R](file: File, result: R) {
    val fos = new FileOutputStream(file)
    val oos = new ObjectOutputStream(fos)
    try {
      oos.writeObject(result)
    } finally {
      fos.close()
      oos.close()
    }
  }

  private def saveFailure(file: File, t: Throwable) {
    val fos = new FileOutputStream(file)
    val oos = new ObjectOutputStream(fos)
    try {
      oos.writeObject(SeparateJvmFailure(t))
    } finally {
      fos.close()
      oos.close()
    }
  }
}
