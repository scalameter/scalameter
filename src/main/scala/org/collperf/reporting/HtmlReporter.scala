package org.collperf
package reporting



import org.jfree.chart._
import java.io._
import collection._
import xml._



case class HtmlReporter(val renderers: HtmlReporter.Renderer*) extends Reporter {

  def head = 
  <head>
    <title>Performance report</title>
    <link type="text/css" media="screen" rel="stylesheet" href="lib/index.css"/>
  </head>

  def body(result: ResultData, persistor: Persistor) =
  <body>
  {machineInformation}
  <h1>Performance test charts</h1>
  {
    for ((module, moduleresults) <- result.curves.orderedGroupBy(_.context.module)) yield <div>
    <h2>Performance test group: {module}</h2>
    {
      for ((method, methodresults) <- moduleresults.orderedGroupBy(_.context.goe(Key.method, ""))) yield <p><div>
        <h3>Method: {method}</h3>
        {
          for (r <- renderers) yield r.render(methodresults)
        }
      </div></p>
    }
    </div>
  }
  </body>

  def machineInformation =
  <div>
  <h1>Machine information</h1>
  <p><ul>
  {
    for ((k, v) <- Context.machine.properties.toList.sortBy(_._1)) yield <li>
    {k + ": " + v}
    </li>
  }
  </ul></p>
  </div>

  def report(results: ResultData, persistor: Persistor) {
    val resultdir = results.context.goe(Key.resultDir, "tmp")

    new File(s"$resultdir/report/images").mkdir()
    new File(s"$resultdir/report/lib").mkdir()

    val report = <html>{head ++ body(results, persistor)}</html>

    val css = getClass.getClassLoader.getResourceAsStream("css/index.css")
    try {
      val reader = new BufferedReader(new InputStreamReader(css))
      printToFile(new File(s"$resultdir/report/lib/index.css")) { p =>
        var line = ""
        while (line != null) {
          p.println(line)
          line = reader.readLine()
        }
      }
    } finally {
      css.close()
    }

    printToFile(new File(s"$resultdir/report/index.html")) {
      _.println(report.toString)
    }
  }

  def printToFile(f: java.io.File)(op: java.io.PrintWriter => Unit) {
    val p = new java.io.PrintWriter(f)
    try { op(p) } finally { p.close() }
  }

}


object HtmlReporter {

  trait Renderer {
    def render(curves: Seq[CurveData]): Node
  }

  object Renderer {
    def all = Seq(Info(), BigO(), Chart(ChartReporter.ChartFactory.XYLine()))

    case class Info() extends Renderer {
      def render(curves: Seq[CurveData]): Node = 
      <div>Info:
      <ul>
      <li>Number of runs: {curves.head.context.goe(Key.benchRuns, "")}</li>
      <li>Aggregator: {curves.head.context.goe(Key.aggregator, "")}</li>
      </ul>
      </div>
    }

    case class BigO() extends Renderer {
      def render(curves: Seq[CurveData]): Node = 
      <div>Big O analysis:
      <ul>
      {
        for (cd <- curves) yield <li>
        {cd.context.goe(Key.curve, "")}: {cd.context.goe(Key.bigO, "(no data)")}
        </li>
      }
      </ul>
      </div>
    }

    case class Chart(factory: ChartReporter.ChartFactory) extends Renderer {
      def render(curves: Seq[CurveData]): Node = {
        val resultdir = curves.head.context.goe(Key.resultDir, "tmp")
        val scopename = curves.head.context.scopeName
        val chart = factory.createChart(scopename, curves)
        val chartfile = new File(s"$resultdir/report/images/$scopename.png")
        ChartUtilities.saveChartAsPNG(chartfile, chart, 1600, 1200)

        <div>
        <a href={"images/" + scopename + ".png"}>
        <img src={"images/" + scopename + ".png"} alt={scopename} width="800" height="600"></img>
        </a>
        </div>
      }
    }
  }

}











