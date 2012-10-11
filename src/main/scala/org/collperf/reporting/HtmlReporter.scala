package org.collperf
package reporting



import org.jfree.chart._
import java.io._
import collection._
import xml._
import utils.Tree



case class HtmlReporter(val renderers: HtmlReporter.Renderer*) extends Reporter {

  val sep = File.separator

  def head = 
    <head>
      <title>Performance report</title>
      <link type="text/css" media="screen" rel="stylesheet" href="lib/index.css"/>
    </head>

  def body(result: Tree[CurveData], persistor: Persistor) = {
    <body>
      {machineInformation}
      <h1>Performance test charts</h1>
      {
        for ((ctx, scoperesults) <- result.scopes; if scoperesults.nonEmpty) yield <p><div>
          <h2>Performance test group: {ctx.scope}</h2>
          {
            val history = persistor.load(ctx)
            for (r <- renderers) yield r.render(ctx, scoperesults, history)
          }
        </div></p>
      }
    </body>
  }

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

  def report(results: Tree[CurveData], persistor: Persistor) {
    val resultdir = results.context.goe(Key.resultDir, "tmp")

    new File(s"$resultdir").mkdir()
    new File(s"$resultdir${sep}report").mkdir()
    new File(s"$resultdir${sep}report${sep}images").mkdir()
    new File(s"$resultdir${sep}report${sep}lib").mkdir()

    val report = <html>{head ++ body(results, persistor)}</html>

    val css = getClass.getClassLoader.getResourceAsStream("css/index.css")
    try {
      val reader = new BufferedReader(new InputStreamReader(css))
      printToFile(new File(s"$resultdir${sep}report${sep}lib${sep}index.css")) { p =>
        var line = ""
        while (line != null) {
          p.println(line)
          line = reader.readLine()
        }
      }
    } finally {
      css.close()
    }

    printToFile(new File(s"$resultdir${sep}report${sep}index.html")) {
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
    def render(context: Context, curves: Seq[CurveData], h: History): Node
  }

  object Renderer {
    def all = Seq(Info(), BigO(), Chart(ChartReporter.ChartFactory.XYLine()))

    case class Info() extends Renderer {
      def render(context: Context, curves: Seq[CurveData], h: History): Node = 
      <div>Info:
      <ul>
      <li>Number of runs: {context.goe(Key.benchRuns, "")}</li>
      <li>Executor: {context.goe(Key.executor, "")}</li>
      </ul>
      </div>
    }

    case class BigO() extends Renderer {
      def render(context: Context, curves: Seq[CurveData], h: History): Node = 
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
      def render(context: Context, curves: Seq[CurveData], h: History): Node = {
        val resultdir = context.goe(Key.resultDir, "tmp")
        val scopename = context.scope
        val chart = factory.createChart(scopename, curves)
        val chartfile = new File(s"$resultdir${File.separator}report${File.separator}images${File.separator}$scopename.png")
        ChartUtilities.saveChartAsPNG(chartfile, chart, 1600, 1200)

        <div>
        <a href={"images/" + scopename + ".png"}>
        <img src={"images/" + scopename + ".png"} alt={scopename} width="800" height="600"></img>
        </a>
        </div>
      }
    }

    case class Regression() extends Renderer {
      def render(context: Context, curves: Seq[CurveData], h: History): Node = {
        // TODO

        <div>History:
        </div>
      }
    }
  }

}











