package org.scalameter
package reporting



import java.util.Date
import java.io._
import java.awt.Color
import util.parsing.json._
import org.jfree.chart._
import collection._
import xml._
import utils.Tree
import Key._



case class HtmlReporter(val renderers: HtmlReporter.Renderer*) extends Reporter {

  val sep = File.separator

  def machineInformation =
    <div>
      <h1>Machine information</h1>
      <ul>
      {
        for ((k, v) <- Context.machine.properties.toList.sortBy(_._1)) yield <li>
        {k + ": " + v}
        </li>
      }
      </ul>
    </div>

  def date(results: Tree[CurveData]) = {
    val dateoption = for {
      start <- results.context.get[Date](reports.startDate)
      end <- results.context.get[Date](reports.endDate)
    } yield <div>
      <div>Started: {start}</div>
      <div>Finished: {end}</div>
      <div>Running time: {(end.getTime - start.getTime) / 1000} seconds</div>
    </div>
    dateoption.getOrElse(<div>No date information.</div>)
  }
  
  def report(result: CurveData, persistor: Persistor) {
    // nothing - the charts are generated only at the end
  }
  
  def copyResource(from: String, to: File) {
    val res = getClass.getClassLoader.getResourceAsStream(from)
    try {
      val buffer = new Array[Byte](1024)
      val fos = new FileOutputStream(to)
      var nBytesRead = 0
      def read = { nBytesRead = res.read(buffer) }
      while ({read; nBytesRead != -1}) {
        fos.write(buffer, 0, nBytesRead)
      }
      if (fos != null) {
        fos.close();
      }
    } finally {
      res.close()
    }
  }

  def JSONIndex(results: Tree[CurveData]) = {
    def JSONCurve(context: Context, curve: CurveData) = JSONObject(
      immutable.Map(
        "scope" -> new JSONArray(curve.context.scopeList),
        "name" -> curve.context.curve,
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

  def report(results: Tree[CurveData], persistor: Persistor) = {
    val resultdir = results.context.goe(reports.resultDir, "tmp")

    new File(s"$resultdir").mkdir()

    val root = new File(s"$resultdir${sep}report")

    root.mkdir()
    new File(root, "css").mkdir()
    new File(root, "img").mkdir()
    new File(root, "js").mkdir()
    new File(root, "js${sep}ScalaMeter").mkdir()

    // val report = <html>{head ++ body(results, persistor)}</html>

    val curvesJSONIndex = JSONIndex(results)

    List(
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
      "js/jquery-ui.custom.min.js",
      "js/ScalaMeter/chart.js",
      "js/ScalaMeter/filter.js",
      "js/ScalaMeter/helper.js",
      "js/ScalaMeter/main.js",
      "js/ScalaMeter/permalink.js"
    ).foreach { filename =>
      copyResource(filename, new File(root, filename))
    }

    def printTsv(pw: PrintWriter) {
      val allCurves = for {
        (ctx, curves) <- results.scopes if curves.nonEmpty
        curve <- curves
      } yield curve

      val separators = "" #:: Stream.continually(", ")

      pw.print("var curveDataTsv = [")
      for ((curve, sep) <- allCurves.toStream zip separators) {
        pw.print(sep)
        pw.print("'")
        DsvReporter.writeCurveData(curve, persistor, pw, "\\n")
        pw.print("'")
      }
      pw.println("];")
    }

    printToFile(new File(root, "js/curves.js")) { pw =>
      pw.println("var ScalaMeter = (function(parent) {");
      pw.println("  var my = { name: \"data\" };");
      pw.println(s"  my.index = $curvesJSONIndex;")
      // printTsv(pw)
      pw.println("  parent[my.name] = my;");
      pw.println("  return parent;");
      pw.println("})(ScalaMeter || {});");
    }

    true
  }

  def printToFile(f: File)(op: PrintWriter => Unit) {
    val p = new PrintWriter(f)
    try { op(p) } finally { p.close() }
  }

}


object HtmlReporter {

  trait Renderer {
    def render(context: Context, curves: Seq[CurveData], hs: Seq[History]): Node
  }

  object Renderer {
    def regression = Seq(HtmlReporter.Renderer.JSChart())

    def basic = Seq(Info(), BigO(), Chart(ChartReporter.ChartFactory.XYLine()))

    case class Info() extends Renderer {
      def render(context: Context, curves: Seq[CurveData], hs: Seq[History]): Node = 
      <div>Info:
      <ul>
      <li>Number of runs: {context.goe(exec.benchRuns, "")}</li>
      <li>Executor: {context.goe(dsl.executor, "")}</li>
      </ul>
      </div>
    }

    case class BigO() extends Renderer {
      def render(context: Context, curves: Seq[CurveData], hs: Seq[History]): Node = 
      <div>Big O analysis:
      <ul>
      {
        for (cd <- curves) yield <li>
        {cd.context.goe(dsl.curve, "")}: {cd.context.goe(reports.bigO, "(no data)")}
        </li>
      }
      </ul>
      </div>
    }

    case class Chart(factory: ChartReporter.ChartFactory, title: String = "Chart", colors: Seq[Color] = Seq()) extends Renderer {
      def render(context: Context, curves: Seq[CurveData], hs: Seq[History]): Node = {
        val resultdir = context.goe(reports.resultDir, "tmp")
        val scopename = context.scope
        val chart = factory.createChart(scopename, curves, hs, colors)
        val chartfile = new File(s"$resultdir${File.separator}report${File.separator}images${File.separator}$scopename.png")
        ChartUtilities.saveChartAsPNG(chartfile, chart, 1600, 1200)

        <div>
        <p>{s"$title :"}</p>
        <a href={"images/" + scopename + ".png"}>
        <img src={"images/" + scopename + ".png"} alt={scopename} width="800" height="600"></img>
        </a>
        </div>
      }
    }
    
    case class JSChart() extends Renderer {
      def render(context: Context, curves: Seq[CurveData], hs: Seq[History]): Node = {
        val resultdir = ".." //context.goe(reports.resultDir, "tmp")
        val group = context.scope
        val sep = "/"

        def addCurve(curve: CurveData): Node = {
          val curveName = curve.context.curve
          val scope = scala.xml.Unparsed(new scala.util.parsing.json.JSONArray(curve.context.scopeList).toString())
          val filename = s"$resultdir$sep$group.$curveName.dsv"
          <script type="text/javascript">
            cd.addGraph({ scope }, { s"'$curveName'" }, { s"'$filename'" });
          </script>
        }
        <div> {
          for (c <- curves) yield addCurve(c)
        } </div>
      }
    }

    case class HistoryList() extends Renderer {
      def render(context: Context, curves: Seq[CurveData], hs: Seq[History]): Node = {
        // TODO

        <div>
        </div>
      }
    }

    case class Regression(tester: RegressionReporter.Tester, colors: Seq[Color] = Seq()) extends Renderer {
      def render(context: Context, curves: Seq[CurveData], hs: Seq[History]): Node = {

        val factory = ChartReporter.ChartFactory.ConfidenceIntervals(true, true, tester)
        val resultdir = context.goe(reports.resultDir, "tmp")
        val scopename = context.scope

        for {
          (curve, history) <- curves zip hs
          (prevdate, prevctx, previousCurve) <- history.results
        } yield {
          val checkedCurve = dyn.log.withValue(Log.None) {
            tester.apply(curve.context, curve, Seq(previousCurve))
          }
          if (!checkedCurve.success) {
            // draw a graph
            val chart = factory.createChart(scopename, Seq(checkedCurve), Seq(History(Seq((prevdate, prevctx, previousCurve)))))
            val chartfile = new File(s"$resultdir${File.separator}report${File.separator}images${File.separator}$scopename.png")
            ChartUtilities.saveChartAsPNG(chartfile, chart, 1600, 1200)
          }
        }

        <div>
        <p>Failed tests:</p>
        <a href={"images/" + scopename + ".png"}>
        <img src={"images/" + scopename + ".png"} alt={scopename} width="800" height="600"></img>
        </a>
        </div>
      }
    }
  }

}










