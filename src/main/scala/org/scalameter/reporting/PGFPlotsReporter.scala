package org.scalameter
package reporting



import java.io._
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.{Date, TimeZone}
import org.scalameter.utils.Tree
import scala.collection._
import scala.Numeric.Implicits._
import scala.Fractional.Implicits._



/** Produces a pgfplots-based tex file that can be embedded into a Latex document.
 */
case class PGFPlotsReporter[T: Fractional](
  height: String = "5.0cm",
  xLabelShift: (String, String) = ("12", "-10"),
  yLabelShift: (String, String) = ("-8", "-12"),
  ybar: String = "0.1pt",
  barWidthPt: Int = 6,
  errorBars: Boolean = true,
  plotColors: Seq[String] = Seq(
    "fill={rgb:red,1;green,4;blue,5},",
    """fill={rgb:red,1;green,3;blue,1},
    postaction={
      pattern color=white,
      pattern=north east lines,
    },""",
    """fill={rgb:red,5;green,1;blue,1},
    postaction={
      pattern color=white,
      pattern=north west lines,
    },""",
    """fill={rgb:red,5;green,1;blue,3},
    postaction={
      pattern color=white,
      pattern=crosshatch,
    },""",
    "fill=white,",
    """fill=white,
    postaction={
      pattern color={rgb:red,1;green,4;blue,5},
      pattern=north west lines,
    },"""
  ),
  referenceCurve: String = "default",
  legend: Boolean = true,
  cutoffs: Set[String] = Set()
) extends Reporter[T] {

  val sep = File.separator

  def report(result: CurveData[T], persistor: Persistor) {
  }

  def report(result: Tree[CurveData[T]], persistor: Persistor) = {
    val resultdir = currentContext(Key.reports.resultDir)

    new File(s"$resultdir").mkdirs()

    def reportScope(scope: (Context, Seq[CurveData[T]])): Unit = {
      val (ctx, curves) = scope
      val filename = s"$resultdir$sep${ctx.scope}.tex"
      var writer: PrintWriter = null

      if (curves.isEmpty) return

      try {
        writer = new PrintWriter(new FileWriter(filename, false))
        writeScope(ctx, curves, writer)
      } finally {
        if (writer != null) writer.close()
      }
  }

  result.scopes.foreach(reportScope)

  true
}

def writeScope(
  ctx: Context, curves: Seq[CurveData[T]], pw: PrintWriter
): Unit = {
  import pw._
  import pw.{print => p}

  val formatter = new DecimalFormat
  formatter.setMaximumFractionDigits(3)
  def formatRound(x: Double): String = formatter.format(x)
  def round1(x: Double): Double = (x * 10).toInt / 10.0
  def round2(x: Double): Double = (x * 100).toInt / 100.0
  def formatCoord(x: Integer): String = {
    if (x > 1000) x / 1000 + "k"
    else x.toString
  }

  val keys = curves.head.measurements.head.params.axisData.keys
  assert(keys.size == 1)
  val xlabel = keys.head.fullName
  val unit = curves.head.measurements.head.units
  val fract = implicitly[Fractional[T]]
  var ymin: Double = Double.MaxValue
  var ymax: Double = Double.MinValue
  val paramvalues = mutable.TreeSet[Integer]()
  for (curve <- curves; m <- curve.measurements) {
    val y = fract.toDouble(m.data.avg)
    if (!cutoffs.contains(curve.context.curve)) {
      ymin = math.min(y, ymin)
      ymax = math.max(y, ymax)
    }
    paramvalues += m.params(xlabel)
  }
  val ymaxUp = math.pow(2, math.ceil(math.log(ymax * 1.05) / math.log(2)))
  val xCoords = paramvalues.toSeq.map(formatCoord).mkString(", ")
  val yTicks = (1 to 8).map(n => formatRound((ymaxUp - 0.0) * n / 8)).mkString(", ")
  val yMaxValue = s"${if (legend) ymaxUp * 1.5 else ymaxUp * 1.2}"
  val header = s"""\\begin{tikzpicture}[scale=0.80]
  \\begin{axis}[
  height=$height,
  every axis plot post/.style={/pgf/number format/fixed},
  ylabel=$$$unit$$,
  xlabel=$$$xlabel$$,
  grid=both,
  every axis y label/.style={
    at={(ticklabel* cs:1.05)},
    xshift=${yLabelShift._1},
    yshift=${yLabelShift._2},
    anchor=south,
  },
  every axis x label/.style={
    at={(ticklabel* cs:1.05)},
    xshift=${xLabelShift._1},
    yshift=${xLabelShift._2},
    anchor=north,
    rotate=-90,
  },
  scaled y ticks=false,
  yticklabel style={
    /pgf/number format/fixed,
  },
  xticklabel style={
    rotate=-90,
  },
  ytick={$yTicks},
  ybar=$ybar,
  bar width=${barWidthPt}pt,
  x=${0.21 * curves.size / 2}cm,
  ymin=0,
  ymax=$yMaxValue,
  x,
  axis on top,
  xtick=data,
  symbolic x coords={
    $xCoords
  },
  % restrict y to domain*=0:75,
  visualization depends on=rawy\\as\\rawy,
  after end axis/.code={
    \\draw[line width=3pt, white, decoration={snake, amplitude=1pt}, decorate] (rel axis cs:0,1.05) -- (rel axis cs:1,1.05);
  },
  axis lines*=left,
  clip=false,
  legend style={
    legend columns=3,
    at={(0.04, 0.98)},
    anchor=north west
  },
  draw opacity=0.4,
  major grid style={
    draw opacity=0.3,
  },
]
    """
    p(header)

    val plots = mutable.LinkedHashMap[
      String,
      (String, immutable.TreeMap[Integer, (Double, Double)])
    ]()
    for ((curve, i) <- curves.zipWithIndex) {
      val addplot = s"""
\\addplot[
  ${plotColors(i)}
  ${if (errorBars) "error bars/.cd," else ""}
  error bar style=red,
  y dir=both,
  y explicit,
]
"""

      var values = immutable.TreeMap[Integer, (Double, Double)]()
      for (m <- curve.measurements) {
        val x = m.params[Integer](xlabel)
        val y = fract.toDouble(m.data.avg)
        val stdev = m.data.stdev
        values = values + (x -> (y -> stdev))
      }
      plots(curve.context.curve) = (addplot, values)
    }

    def cutoff(y: Double): Double = {
      if (y <= ymax) y
      else ymaxUp * 1.15
    }
    for (((name, (addplot, values)), i) <- plots.zipWithIndex) {
      p(addplot)
      p("plot coordinates {\n")
      for ((x, (y, stdev)) <- values) {
        p(s"(${formatCoord(x)}, ${cutoff(y)}) += (0,$stdev) -= (0,$stdev)\n")
      }
      p("};\n")
      p(s"\\addlegendentry{$name}\n")
      for ((x, (y, stdev)) <- values) {
        val xcoord = formatCoord(x)
        var ypos = round2(cutoff(y))
        var cut = false
        if (ypos > ymaxUp) {
          ypos = ymaxUp * 0.7
          cut = true
        }
        val xshift = round1(0.5 + -plots.size / 2.0 + i)
        val referenceY = plots(referenceCurve)._2(x)._1
        val multiplier = round1(y / referenceY)
        p(s"\\node[above] at ($$(axis cs:$xcoord, $ypos)$$) ")
        p(s"[xshift=$xshift*\\pgfkeysvalueof{/pgf/bar width}]\n")
        p(s"{\\rotatebox{-90}{\\scriptsize{$$$multiplier\\times$$}}};\n")
        if (cut) {
          p(s"\\node[above] at ($$(axis cs:$xcoord, $ymaxUp)$$) ")
          p(s"[draw=none,fill=white,minimum width=${barWidthPt + 1}pt,")
          p(s"tape,inner sep=0.1pt]\n")
          p(s"{};\n")
        }
      }
    }

    val footer = s"""
${if (!legend) "\\legend{}" else ""}
\\end{axis}
\\end{tikzpicture}"""
    p(footer)
  }
}
