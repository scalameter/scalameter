package org.scalameter

import org.scalameter.examples._
import org.scalatest.FunSuite
import java.io._


class ResultDirTest extends FunSuite {
  // java.io.File doesn't support recursive delete
  def removeAll(path: String) = {
    def getRecursively(f: File): Seq[File] =
      f.listFiles.filter(_.isDirectory).flatMap(getRecursively).toSeq ++ f.listFiles

    getRecursively(new File(path)).foreach { f => f.delete() }

    new File(path).delete
  }

  test("test setting result directory from command line") {
    try {
      val dir = "_testReport"
      val file = new File(dir)
      if (file.exists) removeAll(file.getPath)

      new RegressionTest main(Array(s"-CresultDir $dir"))

      val report = new File(dir, "report")
      assert(report.exists)

      removeAll(file.getPath)
    } catch { case t: Throwable =>
      t.printStackTrace()
      throw t
    }
  }
}

