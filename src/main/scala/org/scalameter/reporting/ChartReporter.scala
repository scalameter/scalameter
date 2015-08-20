package org.scalameter
package reporting



import java.awt.{BasicStroke, Color}
import java.io._
import java.text.DateFormat.{MEDIUM, getDateTimeInstance}
import java.util.Date
import org.jfree.chart.labels.{ItemLabelAnchor, ItemLabelPosition, StandardCategoryItemLabelGenerator}
import org.jfree.chart.renderer.category.BarRenderer
import org.jfree.chart.renderer.xy.{DeviationRenderer, XYLineAndShapeRenderer}
import org.jfree.chart.{LegendItem, LegendItemCollection}
import org.jfree.data.category.DefaultCategoryDataset
import org.jfree.ui.TextAnchor
import org.scalameter.Key.reports._
import org.scalameter.utils.Statistics._
import org.scalameter.utils.Tree
import scala.Numeric.Implicits._
import scala.collection._
import scala.math.Pi
import scalax.chart.Chart
import scalax.chart.api._



case class ChartReporter[T: Numeric](drawer: ChartReporter.ChartFactory,
  fileNamePrefix: String = "", wdt: Int = 1600, hgt: Int = 1200) extends Reporter[T] {

  /** Does nothing, the charts are generated only at the end. */
  override final def report(result: CurveData[T], persistor: Persistor): Unit = ()

  def report(result: Tree[CurveData[T]], persistor: Persistor) = {
    for ((ctx, curves) <- result.scopes if curves.nonEmpty) {
      val scopename = ctx.scope
      val histories = curves.map(c => persistor.load[T](c.context))
      val chart = drawer.createChart(scopename, curves, histories)
      val dir = result.context(resultDir)
      new File(dir).mkdirs()
      val chartfile = s"$dir/$fileNamePrefix$scopename.png"
      chart.saveAsPNG(chartfile, (wdt,hgt))
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
     *  @param histories        previous chart data for the same set of curves
     *  @param colors         specifies the colors assigned to the the first `colors.size` curves from `cs`.
     *                        The rest of the curves are assigned some default set of colors.
     */
    def createChart[T: Numeric](scopename: String, cs: Seq[CurveData[T]],
      histories: Seq[History[T]], colors: Seq[Color] = Seq()): Chart
  }

  object ChartFactory {

    case class XYLine() extends ChartFactory {
      def createChart[T: Numeric](scopename: String, cs: Seq[CurveData[T]],
        histories: Seq[History[T]], colors: Seq[Color] = Seq()): Chart = {
        val dataset = for ((curve, idx) <- cs.zipWithIndex) yield {
          val seriesName = curve.context.goe(dsl.curve, idx.toString)

          val seriesData = for {
            measurement <- curve.measurements
            x = measurement.params.axisData.head._2.asInstanceOf[Int]
            y = measurement.value
          } yield x -> y

          seriesName -> seriesData
        }

        val chart = XYLineChart(dataset)
        chart.title = scopename
        chart.plot.domain.axis.label = cs.head.measurements.head.params.axisData.head._1.fullName
        chart.plot.range.axis.label = "value"

        chart.plot.setBackgroundPaint(new java.awt.Color(180, 180, 180))
        chart.antiAlias = true

        val renderer = new XYLineAndShapeRenderer()
        for (i <- cs.indices) renderer.setSeriesShapesVisible(i, true)
        chart.plot.setRenderer(renderer)

        chart
      }
    }

    case class ConfidenceIntervals(showLatestCi: Boolean,
      showHistoryCi: Boolean, t: RegressionReporter.Tester) extends ChartFactory {

      private def ciFor[T: Numeric](curve: CurveData[T], values: Seq[T]) = if (showLatestCi) {
        t.confidenceInterval(curve.context, values)
      } else {
        (0D, 0D)
      }

      def createChart[T: Numeric](scopename: String, cs: Seq[CurveData[T]],
        histories: Seq[History[T]], colors: Seq[Color] = Seq()): Chart = {

        def createDataset = {
          val dataset = new YIntervalSeriesCollection
          for ((curve, history) <- cs zip histories) {
            if (history.results.isEmpty) {
              val series = new YIntervalSeries(curve.context(dsl.curve))
              for (measurement <- curve.measurements) {
                val (yLow,yHigh) = ciFor(curve, measurement.complete)
                series.add(measurement.params.axisData.head._2.asInstanceOf[Int],
                  measurement.value.toDouble(), yLow, yHigh)
              }
              dataset.addSeries(series)
            } else {
              val newestSeries = new YIntervalSeries(curve.context(dsl.curve))
              val historySeries = new YIntervalSeries(curve.context(dsl.curve))

              for ((measurement, measurementIndex) <- curve.measurements.zipWithIndex) {
                val x = measurement.params.axisData.head._2.asInstanceOf[Int]

                /* Fetch, for each corresponding curve in history, the measurements that were at the same position (same size for instance)
                on x-axis, and make a list of them */
                val previousMeasurements = for {
                  pastResult <- history.results
                  correspondingCurveInHistory = pastResult._3
                } yield correspondingCurveInHistory.measurements(measurementIndex)
                // We then take all observations that gave the value measurement (by calling complete) of each point, and concat them
                val previousMeasurementsObservations = previousMeasurements flatMap(m => m.complete)

                val (yLowThis,yHighThis) = ciFor(curve, previousMeasurementsObservations)
                val (yLowNewest,yHighNewest) = ciFor(curve, measurement.complete)

                val meanForThisPoint = mean(previousMeasurementsObservations.map(_.toDouble()))
                // Params : x - the x-value, y - the y-value, yLow - the lower bound of the y-interval, yHigh - the upper bound of the y-interval.

                historySeries.add(x, meanForThisPoint, yLowThis, yHighThis)
                newestSeries.add(x, measurement.value.toDouble(), yLowNewest, yHighNewest)
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
          for((color, i) <- colors.zipWithIndex) {
            renderer.setSeriesStroke(i, new BasicStroke(3F, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND))
            renderer.setSeriesPaint(i, color)
            renderer.setSeriesFillPaint(i, color)
          }
          renderer.setAlpha(0.25F)
        }

        val dataset = createDataset
        val chartName = scopename
        val xAxisName = cs.head.measurements.head.params.axisData.head._1.fullName

        // instantiate a DeviationRenderer (lines, shapes)
        val renderer = new DeviationRenderer(true, true)
        paintCurves(renderer)

        val chart = XYLineChart(dataset)
        chart.title = chartName
        chart.plot.domain.axis.label = xAxisName
        chart.plot.range.axis.label = "value"

        chart.plot.setBackgroundPaint(new java.awt.Color(200, 200, 200))
        chart.plot.setRenderer(renderer)
        // There are many other configurable appearance options !
        chart.antiAlias = true
        chart
      }
    }

    /** Returns the data to dataset converter. */
    private implicit def MyToCategoryDatasetConverter[T: Numeric]: ToCategoryDataset[Seq[(String,(String, T))]] =
      ToCategoryDataset { coll =>
        coll.foldLeft(new DefaultCategoryDataset) { case (dataset,(series,(category,value))) =>
          dataset.addValue(value.toDouble(), series, category)
          dataset
        }
      }

    case class TrendHistogram() extends ChartFactory {

      def createChart[T: Numeric](scopename: String, cs: Seq[CurveData[T]],
        histories: Seq[History[T]], colors: Seq[Color] = Seq()): Chart = {
        val now = new Date
        val df = getDateTimeInstance(MEDIUM, MEDIUM)

        /*
         * A History contains the previous curves for a curve in cs. For instance, if we have three dates (categories) on the chart, and for instance
         * three curves for the most recent measurements (so cs has size 3), then histories will have length 3 too (because there are 3 curves), and each
         * History in histories will contain 2 Entry because there are 3 dates (categories).
         *
         * cs and histories will always have the same length here
         *
         * case class History(results: Seq[History.Entry], ...)
         * type Entry = (Date, Context, CurveData)
         * def curves = results.map(_._3)
         * def dates = results.map(_._1)
         */
        val data = for {
          (c, history) <- cs zip histories
          curves = history.curves :+ c
          dates = history.dates :+ now
          categories = dates map df.format
          (curve, category) <- curves zip categories
          measurement <- curve.measurements
          curveName = curve.context(dsl.curve)
          measurementParams = (for(p <- measurement.params.axisData) yield (s"""${p._1.fullName} : ${p._2}""")).mkString("[", ", ", "]")
          series = s"""$curveName $measurementParams"""
        } yield (series,(category,measurement.value))

        val chart = BarChart(data)
        chart.title = scopename
        chart.plot.domain.axis.label = "Date"
        chart.plot.range.axis.label = "Value"

        val plot = chart.plot
        val renderer: BarRenderer = plot.getRenderer.asInstanceOf[BarRenderer]
        renderer.setDrawBarOutline(false)
        renderer.setItemMargin(0D) // to have no space between bars of a same category

        // old version of paintCurves, does not allow custom colors
        /*
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
        }*/

        /*
         * new version. If there are not enough colors specified, the rest are default colors assigned by JFreeChart
         */
        def paintCurves() = {

          def loop(numbersOfMeasurements: Seq[Int], colors: Seq[Color], seriesIndex: Int): Unit = (numbersOfMeasurements, colors) match {

            case (Nil, _) => // do nothing

            case (hn :: tn, Nil) =>
              for (i <- (0 until hn)) {
                val seriesPaint = renderer.lookupSeriesPaint(seriesIndex)
                renderer.setSeriesPaint(seriesIndex + i, seriesPaint)
              }
              loop(tn, Nil, seriesIndex + hn)

            case (hn :: tn, hc :: tc) =>
              for (i <- (0 until hn)) {
                renderer.setSeriesPaint(seriesIndex + i, hc)
              }
              loop(tn, tc, seriesIndex + hn)
          }

          val numbersOfMeasurementsPerCurve = cs map (c => c.measurements.size)
          loop(numbersOfMeasurementsPerCurve, colors, 0)
        }

        def setChartLegend() = {
          var seriesIndex = 0
          val legendItems = new LegendItemCollection
          for((curve, curveIndex) <- cs.zipWithIndex) {
            val curveName = curve.context.goe(dsl.curve, "Curve " + curveIndex.toString)
            val seriesPaint = renderer.lookupSeriesPaint(seriesIndex)
            val numberOfMeasurements = curve.measurements.size
            legendItems.add(new LegendItem(curveName, seriesPaint))
            seriesIndex += numberOfMeasurements
          }
          plot.setFixedLegendItems(legendItems)
        }

        paintCurves()
        setChartLegend()

        class LabelGenerator extends StandardCategoryItemLabelGenerator {
          val serialVersionUID = -7553175765030937177L
          override def generateLabel(categorydataset: CategoryDataset, i: Int, j: Int) = {
            val rowKey = categorydataset.getRowKey(i).toString
            rowKey.substring(rowKey.indexOf("["))
          }
        }

        renderer.setBaseItemLabelGenerator(new LabelGenerator)
        renderer.setBaseItemLabelsVisible(true)
        // ItemLabelPosition params : 1. item label anchor, 2. text anchor, 3. rotation anchor, 4. rotation angle
        val itemLabelPosition = new ItemLabelPosition(ItemLabelAnchor.INSIDE12, TextAnchor.CENTER_RIGHT, TextAnchor.CENTER_RIGHT, -Pi / 2)
        renderer.setBasePositiveItemLabelPosition(itemLabelPosition)
        val altItemLabelPosition = new ItemLabelPosition(ItemLabelAnchor.OUTSIDE12, TextAnchor.CENTER_LEFT, TextAnchor.CENTER_LEFT, -Pi / 2)
        renderer.setPositiveItemLabelPositionFallback(altItemLabelPosition)

        plot.setBackgroundPaint(new java.awt.Color(200, 200, 200))
        plot.setDomainGridlinePaint(Color.white)
        plot.setRangeGridlinePaint(Color.white)
        chart.backgroundPaint = Color.white
        chart.antiAlias = true
        chart
      }
    }

    case class NormalHistogram() extends ChartFactory {
      def createChart[T: Numeric](scopename: String, cs: Seq[CurveData[T]],
        histories: Seq[History[T]], colors: Seq[Color] = Seq()): Chart = {
        val now = new Date
        val df = getDateTimeInstance(MEDIUM, MEDIUM)

        val data = for {
          (c, history) <- cs zip histories
          curves = history.curves :+ c
          dates = history.dates :+ now
          formattedDates = dates map df.format
          (curve, formattedDate) <- curves zip formattedDates
          measurement <- curve.measurements
          curveName = curve.context(dsl.curve)
          measurementParams = (for(p <- measurement.params.axisData) yield (s"""${p._1.fullName} : ${p._2}""")).mkString("[", ", ", "]")
          series = s"""$curveName#$formattedDate"""
        } yield (series,(measurementParams,measurement.value))

        val chart = BarChart(data)
        chart.title = scopename
        chart.plot.domain.axis.label = "Parameters"
        chart.plot.range.axis.label = "Value"

        val plot = chart.plot
        val renderer: BarRenderer = plot.getRenderer.asInstanceOf[BarRenderer]
        renderer.setDrawBarOutline(false)
        renderer.setItemMargin(0D) // to have no space between bars of a same category

        /*def paintCurves = {
          var seriesIndex = 0
          for ((curve, history) <- cs zip histories) {
            val seriesPaint = renderer.lookupSeriesPaint(seriesIndex)
            for (i <- (0 to history.results.size)) {
              renderer.setSeriesPaint(seriesIndex + i, seriesPaint)
            }
            seriesIndex += (history.results.size + 1)
          }
        }*/

        def paintCurves() = {

          def loop(numbersOfEntries: Seq[Int], colors: Seq[Color], seriesIndex: Int): Unit = (numbersOfEntries, colors) match {

            case (Nil, _) => // do nothing

            case (hn :: tn, Nil) =>
              for (i <- (0 until hn)) {
                val seriesPaint = renderer.lookupSeriesPaint(seriesIndex)
                renderer.setSeriesPaint(seriesIndex + i, seriesPaint)
              }
              loop(tn, Nil, seriesIndex + hn)

            case (hn :: tn, hc :: tc) =>
              for (i <- (0 until hn)) {
                renderer.setSeriesPaint(seriesIndex + i, hc)
              }
              loop(tn, tc, seriesIndex + hn)
          }

          val numbersOfEntries = histories map (h => h.results.size + 1)
          loop(numbersOfEntries, colors, 0)
        }

        def setChartLegend() = {
          var seriesIndex = 0
          var curveIndex = 0
          val legendItems = new LegendItemCollection
          for ((curve, history) <- cs zip histories) {
            val curveName = curve.context.goe(dsl.curve, "Curve " + curveIndex.toString)
            val seriesPaint = renderer.lookupSeriesPaint(seriesIndex)
            legendItems.add(new LegendItem(curveName, seriesPaint))
            seriesIndex += (history.results.size + 1)
            curveIndex += 1
          }
          plot.setFixedLegendItems(legendItems)
        }

        paintCurves()
        setChartLegend()

        class LabelGenerator extends StandardCategoryItemLabelGenerator {
          val serialVersionUID = -7553175765030937177L
          override def generateLabel(categorydataset: CategoryDataset, i: Int, j: Int) = {
            val rowKey = categorydataset.getRowKey(i).toString
            rowKey.substring(rowKey.indexOf("#") + 1)
          }
        }

        renderer.setBaseItemLabelGenerator(new LabelGenerator)
        renderer.setBaseItemLabelsVisible(true)
        // ItemLabelPosition params : 1. item label anchor, 2. text anchor, 3. rotation anchor, 4. rotation angle
        val itemLabelPosition = new ItemLabelPosition(ItemLabelAnchor.INSIDE12, TextAnchor.CENTER_RIGHT, TextAnchor.CENTER_RIGHT, -Pi / 2)
        renderer.setBasePositiveItemLabelPosition(itemLabelPosition)
        val altItemLabelPosition = new ItemLabelPosition(ItemLabelAnchor.OUTSIDE12, TextAnchor.CENTER_LEFT, TextAnchor.CENTER_LEFT, -Pi / 2)
        renderer.setPositiveItemLabelPositionFallback(altItemLabelPosition)

        plot.setBackgroundPaint(new java.awt.Color(200, 200, 200))
        plot.setDomainGridlinePaint(Color.white)
        plot.setRangeGridlinePaint(Color.white)
        chart.backgroundPaint = Color.white
        chart.antiAlias = true
        chart
      }
    }

  }

}
