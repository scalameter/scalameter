package org.collperf
package reporting



import org.jfree.data.xy.{XYSeries, XYSeriesCollection}
import org.jfree.data.statistics._
import org.jfree.chart.{ChartFactory => JFreeChartFactory}
import org.jfree.chart.plot.PlotOrientation
import org.jfree.chart._
import java.io._
import collection._
import utils.Tree



case class ChartReporter(fileNamePrefix: String, drawer: ChartReporter.ChartFactory) extends Reporter {

  private[reporting] val defaultChartWidth = 1600
  private[reporting] val defaultChartHeight = 1200

  def report(result: Tree[CurveData], persistor: Persistor) = for ((ctx, curves) <- result.scopes) {
    val scopename = ctx.scope
    val chart = drawer.createChart(scopename, curves)
    val dir = result.context.goe(Key.resultDir, "tmp")
    new File(dir).mkdir()
    val chartfile = new File(s"$dir/$fileNamePrefix$scopename.png")
    ChartUtilities.saveChartAsPNG(chartfile, chart, defaultChartWidth, defaultChartHeight)
  } 

}


object ChartReporter {

  trait ChartFactory {
    def createChart(scopename: String, cs: Seq[CurveData]): JFreeChart
  }

  object ChartFactory {
    case class XYLine() extends ChartFactory {
      def createChart(scopename: String, cs: Seq[CurveData]): JFreeChart = {
        val seriesCollection = new XYSeriesCollection
        val chartName = scopename
        val xAxisName = cs.head.measurements.head.params.axisData.head._1

        for ((curve, idx) <- cs.zipWithIndex) {
          val series = new XYSeries(curve.context.goe(Key.curve, idx.toString), true, true)
          for (measurement <- curve.measurements) {
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



















