
/* d3.tsv("test.dsv", function(error, data) {
 d3.select(".main").selectAll(".clock")
 .data(data).enter()
 .append("div").attr("class", "test")
 .text(function(d) { return JSON.stringify(d); });
 }); */

function isDef(value) {
	return typeof value !== 'undefined'; 
}

var STD_MARGIN = {
	top : 20,
	right : 20,
	bottom : 30,
	left : 50
}

var STD_WIDTH = 700;
var STD_HEIGHT = 400;

//lineGraph("test.dsv", "body", "value [ms]", 960, 500, margin)

function id(d) {
	return d;
}

function addGraph(group, method, dsv) {
	//lineGraph(dsv, "#chart", "value [ms]")
	setGraph(dsv);
	var tree = d3.select(".tree"); 
	var grpNode = tree.selectAll("*").data([group], id);
	grpNode.enter().append("div").text(group);
	var methodNode = grpNode.append("a");
	methodNode.attr("class", "testmethod").text(method);
	methodNode.attr("href", "javascript:setGraph('" + dsv + "')")
}

function setGraph(dsv) {
	d3.select(".chart").selectAll("*").remove();
	lineGraph(dsv, ".chart", "value [ms]");
}

function lineGraph(dsv, node, ylabel, width, height, margin) {
	width = isDef(width) ? width : STD_WIDTH;
	height = isDef(height) ? height : STD_HEIGHT;
	margin = isDef(margin) ? margin : STD_MARGIN;

	var w = width - margin.left - margin.right;
	var h = height - margin.top - margin.bottom;

	var x = d3.scale.linear().range([ 0, w ]);

	var y = d3.scale.linear().range([ h, 0 ]);

	var xAxis = d3.svg.axis().scale(x).orient("bottom");

	var yAxis = d3.svg.axis().scale(y).orient("left");

	var line = d3.svg.line().x(function(d) {
		return x(d["param-size"]);
	}).y(function(d) {
		return y(d.value);
	});

	var svg = d3.select(node).append("svg").attr("width",
			w + margin.left + margin.right).attr("height",
			h + margin.top + margin.bottom).append("g").attr("transform",
			"translate(" + margin.left + "," + margin.top + ")");

	d3.tsv(dsv, function(error, data) {
		data.forEach(function(d) {
			d["param-size"] = +d["param-size"];
			d.value = +d.value;
		});

		x.domain(d3.extent(data, function(d) {
			return d["param-size"];
		}));
		y.domain(d3.extent(data, function(d) {
			return d.value;
		}));

		svg.append("g").attr("class", "x axis").attr("transform",
				"translate(0," + h + ")").call(xAxis);

		svg.append("g").attr("class", "y axis").call(yAxis).append("text")
				.attr("transform", "rotate(-90)").attr("y", 6).attr("dy",
						".71em").style("text-anchor", "end").text(ylabel);

		svg.append("path").datum(data).attr("class", "line").attr("d", line);
	});
}


/*
<div class="chart">
  <div class="title">My Chart</div>
</div>
 */
function barChart() {
    if (!barChart.id) barChart.id = 0;

    var margin = {top: 10, right: 10, bottom: 20, left: 10},
        x,
        y = d3.scale.linear().range([100, 0]),
        id = barChart.id++,
        axis = d3.svg.axis().orient("bottom"),
        brush = d3.svg.brush(),
        brushDirty,
        dimension,
        group,
        round;

    function chart(div) {
      var width = x.range()[1],
          height = y.range()[0];

      y.domain([0, group.top(1)[0].value]);

      div.each(function() {
        var div = d3.select(this),
            g = div.select("g");

        // Create the skeletal chart.
        if (g.empty()) {
          div.select(".title").append("a")
              .attr("href", "javascript:reset(" + id + ")")
              .attr("class", "reset")
              .text("reset")
              .style("display", "none");

          g = div.append("svg")
              .attr("width", width + margin.left + margin.right)
              .attr("height", height + margin.top + margin.bottom)
            .append("g")
              .attr("transform", "translate(" + margin.left + "," + margin.top + ")");

          g.append("clipPath")
              .attr("id", "clip-" + id)
            .append("rect")
              .attr("width", width)
              .attr("height", height);

          g.selectAll(".bar")
              .data(["background", "foreground"])
            .enter().append("path")
              .attr("class", function(d) { return d + " bar"; })
              .datum(group.all());

          g.selectAll(".foreground.bar")
              .attr("clip-path", "url(#clip-" + id + ")");

          g.append("g")
              .attr("class", "axis")
              .attr("transform", "translate(0," + height + ")")
              .call(axis);

          // Initialize the brush component with pretty resize handles.
          var gBrush = g.append("g").attr("class", "brush").call(brush);
          gBrush.selectAll("rect").attr("height", height);
          gBrush.selectAll(".resize").append("path").attr("d", resizePath);
        }

        // Only redraw the brush if set externally.
        if (brushDirty) {
          brushDirty = false;
          g.selectAll(".brush").call(brush);
          div.select(".title a").style("display", brush.empty() ? "none" : null);
          if (brush.empty()) {
            g.selectAll("#clip-" + id + " rect")
                .attr("x", 0)
                .attr("width", width);
          } else {
            var extent = brush.extent();
            g.selectAll("#clip-" + id + " rect")
                .attr("x", x(extent[0]))
                .attr("width", x(extent[1]) - x(extent[0]));
          }
        }

        g.selectAll(".bar").attr("d", barPath);
      });

      function barPath(groups) {
        var path = [],
            i = -1,
            n = groups.length,
            d;
        while (++i < n) {
          d = groups[i];
          path.push("M", x(d.key), ",", height, "V", y(d.value), "h9V", height);
        }
        return path.join("");
      }

      function resizePath(d) {
        var e = +(d == "e"),
            x = e ? 1 : -1,
            y = height / 3;
        return "M" + (.5 * x) + "," + y
            + "A6,6 0 0 " + e + " " + (6.5 * x) + "," + (y + 6)
            + "V" + (2 * y - 6)
            + "A6,6 0 0 " + e + " " + (.5 * x) + "," + (2 * y)
            + "Z"
            + "M" + (2.5 * x) + "," + (y + 8)
            + "V" + (2 * y - 8)
            + "M" + (4.5 * x) + "," + (y + 8)
            + "V" + (2 * y - 8);
      }
    }

    brush.on("brushstart.chart", function() {
      var div = d3.select(this.parentNode.parentNode.parentNode);
      div.select(".title a").style("display", null);
    });

    brush.on("brush.chart", function() {
      var g = d3.select(this.parentNode),
          extent = brush.extent();
      if (round) g.select(".brush")
          .call(brush.extent(extent = extent.map(round)))
        .selectAll(".resize")
          .style("display", null);
      g.select("#clip-" + id + " rect")
          .attr("x", x(extent[0]))
          .attr("width", x(extent[1]) - x(extent[0]));
      dimension.filterRange(extent);
    });

    brush.on("brushend.chart", function() {
      if (brush.empty()) {
        var div = d3.select(this.parentNode.parentNode.parentNode);
        div.select(".title a").style("display", "none");
        div.select("#clip-" + id + " rect").attr("x", null).attr("width", "100%");
        dimension.filterAll();
      }
    });

    chart.margin = function(_) {
      if (!arguments.length) return margin;
      margin = _;
      return chart;
    };

    chart.x = function(_) {
      if (!arguments.length) return x;
      x = _;
      axis.scale(x);
      brush.x(x);
      return chart;
    };

    chart.y = function(_) {
      if (!arguments.length) return y;
      y = _;
      return chart;
    };

    chart.dimension = function(_) {
      if (!arguments.length) return dimension;
      dimension = _;
      return chart;
    };

    chart.filter = function(_) {
      if (_) {
        brush.extent(_);
        dimension.filterRange(_);
      } else {
        brush.clear();
        dimension.filterAll();
      }
      brushDirty = true;
      return chart;
    };

    chart.group = function(_) {
      if (!arguments.length) return group;
      group = _;
      return chart;
    };

    chart.round = function(_) {
      if (!arguments.length) return round;
      round = _;
      return chart;
    };

    return d3.rebind(chart, brush, "on");
  }
