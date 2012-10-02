package org.collperf
package reporters



import org.jfree.data.xy.{XYSeries, XYSeriesCollection}
import org.jfree.chart.{ChartFactory}
import org.jfree.chart.plot.PlotOrientation
import org.jfree.chart.ChartUtilities
import java.io._
import collection._



class XYChartReporter extends Reporter {

  private val allresults = mutable.Map[String, Set[Result]]() withDefaultValue immutable.Set()
  private val defaultChartHeight = 600
  private val defaultChartWidth = 800

  def report(result: Result, persistor: Persistor) {
    allresults(result.context.scopeName) += result
  }

  def flush() {
    for ((scopename, rs) <- allresults) {
      val seriesCollection = new XYSeriesCollection
      val chartName = scopename
      val xAxisName = rs.head.measurements.head.params.axisData.head._1

      for ((result, idx) <- rs.zipWithIndex) {
        val series = new XYSeries(result.context.getOrElse(Key.curve, idx.toString), true, true)
        for (measurement <- result.measurements) {
          series.add(measurement.params.axisData.head._2.asInstanceOf[Int], measurement.time)
        }
        seriesCollection.addSeries(series)
      }

      val chart = ChartFactory.createXYLineChart(chartName, xAxisName, "time", seriesCollection, PlotOrientation.VERTICAL, true, true, false)
      ChartUtilities.saveChartAsPNG(new File(s"$chartName.png"), chart, defaultChartWidth, defaultChartHeight)
    }
  }

}
