package org.scalameter
package reporting



import java.util.Date
import java.io._
import java.awt.Color
import org.jfree.chart._
import collection._
import xml._
import utils.Tree
import Key._



case class HtmlReporter(val renderers: HtmlReporter.Renderer*) extends Reporter {

  val sep = File.separator

  def head = 
    <head>
      <title>Performance report</title>
      <link type="text/css" media="screen" rel="stylesheet" href="lib/index.css"/>
      <script type="text/javascript" src="lib/d3.v3.min.js"></script>
  	  <script type="text/javascript" src="lib/parse.js"></script>
    </head>

  def body(result: Tree[CurveData], persistor: Persistor) = {
    <body>
      {machineInformation}
      {date(result)}
      <div class="tree"></div>
      <div class="chart"></div>
      <h1>Performance test charts</h1>
      {
        for ((ctx, scoperesults) <- result.scopes; if scoperesults.nonEmpty) yield <div>
          <h2>Performance test group: {ctx.scope}</h2>
          {
            val histories = scoperesults.map(cd => persistor.load(cd.context))
            for (r <- renderers) yield r.render(ctx, scoperesults, histories)
          }
        </div>
      }
    </body>
  }

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
      val reader = new BufferedReader(new InputStreamReader(res))
      printToFile(to) { p =>
        var line = ""
        while (line != null) {
          p.println(line)
          line = reader.readLine()
        }
      }
    } finally {
      res.close()
    }
  }

  def report(results: Tree[CurveData], persistor: Persistor) = {
    val resultdir = results.context.goe(reports.resultDir, "tmp")

    new File(s"$resultdir").mkdir()
    new File(s"$resultdir${sep}report").mkdir()
    new File(s"$resultdir${sep}report${sep}images").mkdir()
    new File(s"$resultdir${sep}report${sep}lib").mkdir()

    val report = <html>{head ++ body(results, persistor)}</html>

    val libdir = new File(s"$resultdir${sep}report${sep}lib")
    copyResource("css/index.css", new File(libdir, "index.css"))
    copyResource("js/d3.v3.min.js", new File(libdir, "d3.v3.min.js"))
    copyResource("js/parse.js", new File(libdir, "parse.js"))

    printToFile(new File(s"$resultdir${sep}report${sep}index.html")) {
      _.println("<!DOCTYPE html>\n" + report.toString)
    }

    true
  }

  def printToFile(f: java.io.File)(op: java.io.PrintWriter => Unit) {
    val p = new java.io.PrintWriter(f)
    try { op(p) } finally { p.close() }
  }

}


object HtmlReporter {

  trait Renderer {
    def render(context: Context, curves: Seq[CurveData], hs: Seq[History]): Node
  }

  object Renderer {
    def regression = Seq(Info(), Chart(ChartReporter.ChartFactory.XYLine()), Chart(ChartReporter.ChartFactory.TrendHistogram()))

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
          val filename = s"$resultdir$sep$group.$curveName.dsv"
          <script type="text/javascript">
            addGraph({ s"'$group'" }, { s"'$curveName'" }, { s"'$filename'" })
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










