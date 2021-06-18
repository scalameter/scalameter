package org.scalameter
package reporting

import java.io._
import org.scalameter.Key._
import org.scalameter.utils.Tree
import scala.collection._
import scala.collection.JavaConverters._
import scala.util.parsing.json.{JSONObject, JSONArray}


case class HtmlReporter[T: Numeric](embedDsv: Boolean = true) extends Reporter[T] {
  import HtmlReporter._

  def report(result: CurveData[T], persistor: Persistor): Unit = {
    // nothing - the charts are generated only at the end
  }

  def report(results: Tree[CurveData[T]], persistor: Persistor) = {
    if (!embedDsv) {
      new DsvReporter(dsvDelimiter).report(results, persistor)
    }

    // resultDir is global setting
    val resultdir = currentContext(reports.resultDir)
    val root = new File(resultdir, "report")
    root.mkdirs()

    val curvesJSONIndex = JSONIndex(results)

    resourceDirs.foreach {
      new File(root, _).mkdirs()
    }

    resourceFiles.foreach { filename =>
      copyResource(filename, new File(root, filename))
    }

    printToFile(new File(root, jsDataFile)) { pw =>
      pw.println("var ScalaMeter = (function(parent) {")
      pw.println("  var my = { name: \"data\" };")
      pw.println(s"  my.index = $curvesJSONIndex;")
      if (embedDsv) {
        printTsv(results, persistor, pw)
      }
      pw.println("  parent[my.name] = my;")
      pw.println("  return parent;")
      pw.println("})(ScalaMeter || {});")
    }

    true
  }

  def machineInformation =
    <div>
      <h1>Machine information</h1>
      <ul>
      {
        for ((k, v) <- Context.machine.properties.toList.sortBy(_._1)) yield <li>
        {s"$k: $v"}
        </li>
      }
      </ul>
    </div>

  def date(results: Tree[CurveData[T]]) = {
    val dateoption = for {
      start <- results.context(reports.startDate)
      end <- results.context(reports.endDate)
    } yield <div>
      <div>Started: {start}</div>
      <div>Finished: {end}</div>
      <div>Running time: {(end.getTime - start.getTime) / 1000} seconds</div>
    </div>
    dateoption.getOrElse(<div>No date information.</div>)
  }

  def JSONIndex(results: Tree[CurveData[T]]) = {
    def JSONCurve(context: Context, curve: CurveData[T]) = JSONObject(
      immutable.Map(
        "scope" -> new JSONArray(curve.context.scopeList),
        "name" -> curve.context.curve,
        "unit" -> curve.context.unit,
        "file" -> s"../${context.scope}.${curve.context.curve}.dsv"
      )
    )

    val JSONCurves = for {
      (ctx, curves) <- results.scopes if curves.nonEmpty
      curve <- curves
    } yield {
      JSONCurve(ctx, curve)
    }
    new JSONArray(JSONCurves.toList)
  }

  def printTsv(results: Tree[CurveData[T]], persistor: Persistor, pw: PrintWriter): Unit = {
    val allCurves = for {
      (ctx, curves) <- results.scopes if curves.nonEmpty
      curve <- curves
    } yield curve

    val separators = "" #:: Stream.continually(", ")

    pw.print("  my.tsvData = [")
    for ((curve, sep) <- allCurves.toStream zip separators) {
      pw.print(sep)
      pw.print("'")
      DsvReporter.writeCurveData(curve, persistor, pw, dsvDelimiter, "\\n")
      pw.print("'")
    }
    pw.println("];")
  }

}


object HtmlReporter {
  val resourceDirs = List("css", "img", "js", "js/ScalaMeter")

  val resourceFiles = List(
    "index.html",
    "css/bootstrap.min.css",
    "css/bootstrap-slider.css",
    "css/icons.gif",
    "css/index.css",
    "css/jquery-ui-1.10.3.custom.css",
    "css/ui.dynatree.css",
    "css/vline.gif",
    "img/arrow.png",
    "img/glyphicons-halflings.png",
    "js/bootstrap.min.js",
    "js/crossfilter.min.js",
    "js/d3.v3.min.js",
    "js/jquery.dynatree.js",
    "js/jquery-1.9.1.js",
    "js/jquery-compat.js",
    "js/jquery-ui-1.10.3.custom.min.js",
    "js/ScalaMeter/chart.js",
    "js/ScalaMeter/dimensions.js",
    "js/ScalaMeter/filter.js",
    "js/ScalaMeter/helper.js",
    "js/ScalaMeter/main.js",
    "js/ScalaMeter/permalink.js" )

  val jsDataFile = "js/ScalaMeter/data.js"

  val dsvDelimiter = '\t'

  def copyResource(from: String, to: File): Unit = {
    val resources = getClass.getClassLoader.getResources(from).asScala
    val resource = resources.find { url =>
      url.getPath.contains("scalameter")
    }

    val res = resource match {
      case Some(url) => url.openStream()
      case None => throw new IllegalStateException(s"Could not find any resource with name $from.")
    }
    try {
      val buffer = new Array[Byte](1024)
      val fos = new FileOutputStream(to)
      var nBytesRead = 0
      def read() = {
        nBytesRead = res.read(buffer)
        nBytesRead != -1
      }
      while (read()) {
        fos.write(buffer, 0, nBytesRead)
      }
      if (fos != null) {
        fos.close()
      }
    } finally {
      res.close()
    }
  }

  def printToFile(f: File)(op: PrintWriter => Unit): Unit = {
    val p = new PrintWriter(f)
    try { op(p) } finally { p.close() }
  }

}
