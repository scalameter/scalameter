package org.collperf
package reporters



import org.jfree.data.xy.{XYSeries, XYSeriesCollection}
import org.jfree.data.statistics._
import org.jfree.chart.{ChartFactory => JFreeChartFactory}
import org.jfree.chart.plot.PlotOrientation
import org.jfree.chart._
import java.io._
import collection._



case class ChartReporter(fileNamePrefix: String, drawer: ChartReporter.ChartFactory) extends Reporter {

  private[reporters] val defaultChartWidth = 1600
  private[reporters] val defaultChartHeight = 1200

  def report(results: Seq[Result], persistor: Persistor) = {
    val scopename = results.head.context.scopeName
    val chart = drawer.createChart(scopename, results)
    new File("tmp").mkdir()
    val chartfile = new File(s"tmp/$fileNamePrefix$scopename.png")
    ChartUtilities.saveChartAsPNG(chartfile, chart, defaultChartWidth, defaultChartHeight)
  } 

}


object ChartReporter {

  trait ChartFactory {
    def createChart(scopename: String, rs: Seq[Result]): JFreeChart
  }

  object ChartFactory {
    case class XYLine() extends ChartFactory {
      def createChart(scopename: String, rs: Seq[Result]): JFreeChart = {
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

        val chart = JFreeChartFactory.createXYLineChart(chartName, xAxisName, "time", seriesCollection, PlotOrientation.VERTICAL, true, true, false)
        chart.getPlot.setBackgroundPaint(new java.awt.Color(200, 200, 200))
        chart.setAntiAlias(true)

        chart
      } 
    }
  }

}



















