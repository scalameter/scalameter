package org.collperf
package reporters



import java.io._
import collection._



class HtmlReporter extends Reporter {

  private val chartreporter = new XYCurveReporter

  def report(result: Result, persistor: Persistor) {
    chartreporter.report(result, persistor)
  }

  def flush() {
    val charts = chartreporter.createCharts()
    chartreporter.allresults.clear()

    val report =
      <html>
        <head>
          <title>Performance report</title>
          <link type="text/css" media="screen" rel="stylesheet" href="lib/index.css"/>
        </head>
        <body>
          <h1>Machine information</h1>
          <p><ul>
          {
            for ((k, v) <- Context.machine.properties) yield <li>
              {k + ": " + v}
            </li>
          }
          </ul></p>
          <h1>Performance test charts</h1>
          {
            for ((module, cs) <- charts.groupBy(_.module)) yield <div>
              <h2>Performance test group: {module}</h2>
              {
                for (c <- cs) yield <p><div>
                <h3>Method: {c.context.goe(Key.method, "")}</h3>
                <div>Info:
                  <ul>
                  <li>Number of runs: {c.context.goe(Key.benchRuns, "")}</li>
                  <li>Aggregator: {c.context.goe(Key.aggregator, "")}</li>
                  </ul>
                </div>
                <div>Big O analysis:
                  <ul>
                  {
                    for (r <- c.results) yield <li>
                      {r.context.goe(Key.curve, "")}: {r.context.goe(Key.bigO, "(no data)")}
                    </li>
                  }
                  </ul>
                </div>
                <div>
                  <a href={"images/" + c.file.getName}>
                    <img src={"images/" + c.file.getName} alt={c.name} width="800" height="600"></img>
                  </a>
                </div>
                </div></p>
              }
            </div>
          }
        </body>
      </html>

    new File("tmp/report/images").mkdir()
    for (c <- charts) c.file.renameTo(new File(s"tmp/report/images/${c.file.getName}"))
    new File("tmp/report/lib").mkdir()
    val css = getClass.getClassLoader.getResourceAsStream("css/index.css")
    try {
      val reader = new BufferedReader(new InputStreamReader(css))
      printToFile(new File("tmp/report/lib/index.css")) { p =>
        var line = ""
        while (line != null) {
          p.println(line)
          line = reader.readLine()
        }
      }
    } finally {
      css.close()
    }

    printToFile(new File("tmp/report/index.html")) {
      _.println(report.toString)
    }
  }

  def printToFile(f: java.io.File)(op: java.io.PrintWriter => Unit) {
    val p = new java.io.PrintWriter(f)
    try { op(p) } finally { p.close() }
  }

}













