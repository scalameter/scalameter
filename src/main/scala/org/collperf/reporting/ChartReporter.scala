package org.collperf
package reporting



import org.jfree.data.xy.{XYSeries, XYSeriesCollection, YIntervalSeriesCollection, YIntervalSeries}
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer
import org.jfree.chart.renderer.xy.DeviationRenderer
import org.jfree.chart.plot.XYPlot
import org.jfree.data.statistics._
import org.jfree.chart.{ChartFactory => JFreeChartFactory}
import org.jfree.chart.plot.PlotOrientation
import org.jfree.chart._
import java.io._
import collection._
import utils.Tree
import java.awt.BasicStroke
import java.awt.Color


case class ChartReporter(fileNamePrefix: String, drawer: ChartReporter.ChartFactory) extends Reporter {

  private[reporting] val defaultChartWidth = 1600
  private[reporting] val defaultChartHeight = 1200

  def report(result: Tree[CurveData], persistor: Persistor) = for ((ctx, curves) <- result.scopes) {
    val scopename = ctx.scope
    val history = persistor.load(ctx)
    val chart = drawer.createChart(scopename, curves, history)
    val dir = result.context.goe(Key.resultDir, "tmp")
    new File(dir).mkdir()
    val chartfile = new File(s"$dir/$fileNamePrefix$scopename.png")
    ChartUtilities.saveChartAsPNG(chartfile, chart, defaultChartWidth, defaultChartHeight)
  } 

}


object ChartReporter {

  trait ChartFactory {
    /** Generates a chart for the given curve data, with the given history.
     *
     *  @param scopename      name of the chart
     *  @param cs             a list of curves that should appear on the chart
     *  @param history        previous chart data for the same set of curves
     */
    def createChart(scopename: String, cs: Seq[CurveData], history: History): JFreeChart
  }

  object ChartFactory {

    case class XYLine() extends ChartFactory {
      def createChart(scopename: String, cs: Seq[CurveData], history: History): JFreeChart = {
        val seriesCollection = new XYSeriesCollection
        val chartName = scopename
        val xAxisName = cs.head.measurements.head.params.axisData.head._1
        val renderer = new XYLineAndShapeRenderer()

        for ((curve, idx) <- cs.zipWithIndex) {
          val series = new XYSeries(curve.context.goe(Key.curve, idx.toString), true, true)
          for (measurement <- curve.measurements) {
            series.add(measurement.params.axisData.head._2.asInstanceOf[Int], measurement.time)
          }
          seriesCollection.addSeries(series)
          renderer.setSeriesShapesVisible(idx, true)
        }

        val chart = JFreeChartFactory.createXYLineChart(chartName, xAxisName, "time", seriesCollection, PlotOrientation.VERTICAL, true, true, false)
        chart.getPlot.setBackgroundPaint(new java.awt.Color(180, 180, 180))
        chart.getPlot.asInstanceOf[XYPlot].setRenderer(renderer)
        chart.setAntiAlias(true)

        chart
      }
    }

    case class Regression(showLatestCi: Boolean, showHistoryCi: Boolean, color: Color) extends ChartFactory {
      def createChart(scopename: String, cs: Seq[CurveData], history: History): JFreeChart = {
        val dataset = new YIntervalSeriesCollection
        val chartName = scopename
        val xAxisName = cs.head.measurements.head.params.axisData.head._1

        // instantiate a DeviationRenderer (lines, shapes)
        val renderer = new DeviationRenderer(true, true)
        // fill the dataset
        for((curve, index) <- cs.zipWithIndex) {
          val series = new YIntervalSeries(curve.context.goe(Key.curve, index.toString))
          for(measurement <- curve.measurements) {

            // Params : x - the x-value, y - the y-value, yLow - the lower bound of the y-interval, yHigh - the upper bound of the y-interval.
            series.add(measurement.params.axisData.head._2.asInstanceOf[Int], measurement.time, 0, 0) // missing these two last params, but confused !
            renderer.setSeriesStroke(index, new BasicStroke(3F, 1, 1))
            renderer.setSeriesFillPaint(index, new Color(0, 0, 0)) // how to choose the colors here ?
          }
          dataset.addSeries(series)
        }

        //String title, String xAxisLabel, String yAxisLabel, XYDataset dataset, PlotOrientation orientation, boolean legend,boolean tooltips, boolean urls
        val chart = JFreeChartFactory.createXYLineChart(chartName, xAxisName, "time", dataset, PlotOrientation.VERTICAL, true, true, false)
        val plot: XYPlot = chart.getPlot.asInstanceOf[XYPlot]
        plot.setBackgroundPaint(new java.awt.Color(200, 200, 200))
        plot.setRenderer(renderer)
        // There are many other configurable appearance options !
        chart.setAntiAlias(true)
        chart
      }
    }

  }

}



















