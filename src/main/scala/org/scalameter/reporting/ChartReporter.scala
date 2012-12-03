package org.scalameter
package reporting



import org.jfree.data.xy.{XYSeries, XYSeriesCollection, YIntervalSeriesCollection, YIntervalSeries}
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer
import org.jfree.chart.renderer.xy.DeviationRenderer
import org.jfree.chart.renderer.category.BarRenderer
import org.jfree.chart.plot.{XYPlot, CategoryPlot}
import org.jfree.data.statistics._
import org.jfree.chart.{ChartFactory => JFreeChartFactory}
import org.jfree.chart.plot.PlotOrientation
import org.jfree.data.category.DefaultCategoryDataset
import org.jfree.chart._
import java.io._
import collection._
import utils.Tree
import utils.Statistics._
import java.awt.BasicStroke
import java.awt.Color
import Key.reports._
import java.text.DateFormat.{getDateTimeInstance, MEDIUM}
import java.util.Date
import org.jfree.chart.{LegendItemCollection, LegendItem}



case class ChartReporter(drawer: ChartReporter.ChartFactory, fileNamePrefix: String = "", wdt: Int = 1600, hgt: Int = 1200) extends Reporter {

  def report(result: CurveData, persistor: Persistor) {
    // nothing - the charts are generated only at the end
  }

  def report(result: Tree[CurveData], persistor: Persistor) = {
    for ((ctx, curves) <- result.scopes if curves.nonEmpty) {
      val scopename = ctx.scope
      val histories = curves.map(c => persistor.load(c.context))
      val chart = drawer.createChart(scopename, curves, histories)
      val dir = result.context.goe(resultDir, "tmp")
      new File(dir).mkdir()
      val chartfile = new File(s"$dir/$fileNamePrefix$scopename.png")
      ChartUtilities.saveChartAsPNG(chartfile, chart, wdt, hgt)
    }
    
    true
  } 

}


object ChartReporter {

  import Key._

  trait ChartFactory {
    /** Generates a chart for the given curve data, with the given history.
     *
     *  @param scopename      name of the chart
     *  @param cs             a list of curves that should appear on the chart
     *  @param history        previous chart data for the same set of curves
     *  @param colors        specifies the colors assigned to the the first `colors.size` curves from `cs`.
     *                        The rest of the curves are assigned some default set of colors.
     */
    def createChart(scopename: String, cs: Seq[CurveData], histories: Seq[History], colors: Seq[Color] = Seq()): JFreeChart
  }

  object ChartFactory {

    case class XYLine() extends ChartFactory {
      def createChart(scopename: String, cs: Seq[CurveData], histories: Seq[History], colors: Seq[Color] = Seq()): JFreeChart = {
        val seriesCollection = new XYSeriesCollection
        val chartName = scopename
        val xAxisName = cs.head.measurements.head.params.axisData.head._1
        val renderer = new XYLineAndShapeRenderer()

        for ((curve, idx) <- cs.zipWithIndex) {
          val series = new XYSeries(curve.context.goe(dsl.curve, idx.toString), true, true)
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

    case class ConfidenceIntervals(showLatestCi: Boolean, showHistoryCi: Boolean, alpha: Double) extends ChartFactory {

      def createChart(scopename: String, cs: Seq[CurveData], histories: Seq[History], colors: Seq[Color] = Seq()): JFreeChart = {

        def createDataset = {
          val dataset = new YIntervalSeriesCollection
          for ((curve, history) <- cs zip histories) {
            if (history.results.isEmpty) {
              val series = new YIntervalSeries(curve.context.goe(dsl.curve, ""))
              for (measurement <- curve.measurements) {
                val ciForThisPoint = if (showLatestCi) {
                  confidenceInterval(measurement.complete, alpha)
                } else {
                  (0D, 0D)
                }
                series.add(measurement.params.axisData.head._2.asInstanceOf[Int], measurement.time, ciForThisPoint._1, ciForThisPoint._2)
              }
            } else {
              val newestSeries = new YIntervalSeries(curve.context.goe(dsl.curve, ""))
              val historySeries = new YIntervalSeries(curve.context.goe(dsl.curve, ""))

              for ((measurement, measurementIndex) <- curve.measurements.zipWithIndex) {
                /* Fetch, for each corresponding curve in history, the measurements that were at the same position (same size for instance)
                on x-axis, and make a list of them */
                val previousMeasurements = for {
                  pastResult <- history.results
                  correspondingCurveInHistory = pastResult._3
                } yield correspondingCurveInHistory.measurements(measurementIndex)
                // We then take all observations that gave the time measurement (by calling complete) of each point, and concat them
                val previousMeasurementsObservations = previousMeasurements flatMap(m => m.complete)

                val ciForThisPoint = if (showHistoryCi) { confidenceInterval(previousMeasurementsObservations, alpha) } else { (0D, 0D) }
                val meanForThisPoint = mean(previousMeasurementsObservations)
                // Params : x - the x-value, y - the y-value, yLow - the lower bound of the y-interval, yHigh - the upper bound of the y-interval.
                historySeries.add(measurement.params.axisData.head._2.asInstanceOf[Int], meanForThisPoint, ciForThisPoint._1, ciForThisPoint._2)

                val ciForNewestPoint = if (showLatestCi) {
                  confidenceInterval(measurement.complete, alpha)
                } else {
                  (0D, 0D)
                }

                newestSeries.add(measurement.params.axisData.head._2.asInstanceOf[Int], measurement.time, ciForNewestPoint._1, ciForNewestPoint._2)
              }
              dataset.addSeries(historySeries)
              dataset.addSeries(newestSeries)
            }
          }

          dataset
        }
        /* We may want to call other methods from the JFreeChart API, as there are a
           lot of them related to appearance in class DeviationRenderer and in its parent classes */
        def paintCurves(renderer: DeviationRenderer) {
          //val test = List(new Color(255, 200, 200), new Color(200, 200, 255))
          for((color, i) <- colors.zipWithIndex) {
            renderer.setSeriesStroke(i, new BasicStroke(3F, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND))
            renderer.setSeriesPaint(i, color)
            renderer.setSeriesFillPaint(i, color)
          }
          renderer.setAlpha(0.25F)
        }

        val dataset = createDataset
        val chartName = scopename
        val xAxisName = cs.head.measurements.head.params.axisData.head._1

        // instantiate a DeviationRenderer (lines, shapes)
        val renderer = new DeviationRenderer(true, true)
        paintCurves(renderer)
    
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

    case class TrendHistogram() extends ChartFactory {
      def createChart(scopename: String, cs: Seq[CurveData], histories: Seq[History], colors: Seq[Color] = Seq()): JFreeChart = {
        val chartName = scopename
        val xAxisName = "Date"
        val now = new Date
        val df = getDateTimeInstance(MEDIUM, MEDIUM)
        val currentDate = df format now
        val dataset = new DefaultCategoryDataset

        for ((c, history) <- cs zip histories) {
          val curves = history.curves :+ c
          val dates = history.dates :+ now
          val categoryNames = dates.map(df format _)
          for ((curve, categoryName) <- curves zip categoryNames) {
            for((measurement, measurementIndex) <- curve.measurements.zipWithIndex) {
              val curveName = curve.context.goe(dsl.curve, "")
              val measurementParams = (for(p <- measurement.params.axisData) yield (p._1.toString + " : " + p._2.toString)).mkString("[", ", ", "]")
              val seriesName: String = curveName + " " + measurementParams
              dataset.addValue(measurement.time, seriesName, categoryName)
            }
          }
        }

        val chart = JFreeChartFactory.createBarChart(chartName, xAxisName, "time", dataset, PlotOrientation.VERTICAL, true, true, false)
        val plot: CategoryPlot = chart.getPlot.asInstanceOf[CategoryPlot]
        val renderer: BarRenderer = plot.getRenderer.asInstanceOf[BarRenderer]
        renderer.setDrawBarOutline(false)
        renderer.setItemMargin(0D) // to have no space between bars of a same category
        
        /*val numberOfCurves = cs.size
        val numberOfSeriesPerCategory = dataset.getRowCount*/

        def paintCurves = {
          var seriesIndex = 0
          for(curve <- cs) {
            val seriesPaint = renderer.lookupSeriesPaint(seriesIndex)
            val numberOfMeasurements = curve.measurements.size
            for (i <- (0 until numberOfMeasurements)) {
              renderer.setSeriesPaint(seriesIndex + i, seriesPaint)
            }
            seriesIndex += numberOfMeasurements
          }
        }

        def setChartLegend = {
          var seriesIndex = 0
          val legendItems = new LegendItemCollection
          for((curve, curveIndex) <- cs.zipWithIndex) {
            val curveName = curve.context.goe(dsl.curve, curveIndex.toString)
            val seriesPaint = renderer.lookupSeriesPaint(seriesIndex)
            val numberOfMeasurements = curve.measurements.size
            legendItems.add(new LegendItem(curveName, seriesPaint))
            seriesIndex += numberOfMeasurements
          }
          plot.setFixedLegendItems(legendItems)
        }

        paintCurves
        setChartLegend

        plot.setBackgroundPaint(new java.awt.Color(200, 200, 200))
        plot.setDomainGridlinePaint(Color.white)
        plot.setRangeGridlinePaint(Color.white)
        chart.setBackgroundPaint(Color.white)
        
        chart.setAntiAlias(true)
        chart
      }
    }

  }

}



















