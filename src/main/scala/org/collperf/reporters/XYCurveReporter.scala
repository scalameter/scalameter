package org.collperf
package reporters



import org.jfree.data.xy.{XYSeries, XYSeriesCollection}
import org.jfree.chart.{ChartFactory}
import org.jfree.chart.plot.PlotOrientation
import org.jfree.chart.ChartUtilities
import java.io._
import collection._



class XYCurveReporter(val fileNamePrefix: String) extends Reporter {

  def this() = this("")

  private[reporters] val allresults = mutable.LinkedHashMap[String, Set[Result]]() withDefaultValue immutable.ListSet()
  private val defaultChartWidth = 1600
  private val defaultChartHeight = 1200

  def report(result: Result, persistor: Persistor) {
    allresults(result.context.scopeName) += result
  }

  case class Chart(name: String, file: File, context: Context, results: Set[Result]) {
    def module = context.goe(Key.module, "")
  }

  private[reporters] def createCharts(): Iterable[Chart] = {
    for ((scopename, rs) <- allresults) yield {
      val seriesCollection = new XYSeriesCollection
      val chartName = scopename
      val xAxisName = rs.head.measurements.head.params.axisData.head._1

      for ((result, idx) <- rs.zipWithIndex) {
        val series = new XYSeries(result.context.goe(Key.curve, idx.toString), true, true)
        for (measurement <- result.measurements) {
          series.add(measurement.params.axisData.head._2.asInstanceOf[Int], measurement.time)
        }
        seriesCollection.addSeries(series)
      }

      val chart = ChartFactory.createXYLineChart(chartName, xAxisName, "time", seriesCollection, PlotOrientation.VERTICAL, true, true, false)
      chart.getPlot.setBackgroundPaint(new java.awt.Color(200, 200, 200))
      chart.setAntiAlias(true)

      new File("tmp").mkdir()
      val chartfile = new File(s"tmp/$fileNamePrefix$chartName.png")
      ChartUtilities.saveChartAsPNG(chartfile, chart, defaultChartWidth, defaultChartHeight)
      Chart(chartName, chartfile, rs.head.context, rs)
    }
  }

  def flush() {
    createCharts()
    allresults.clear()
  }

}
