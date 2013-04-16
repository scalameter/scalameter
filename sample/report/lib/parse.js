function isDef(value) {
	return typeof value !== 'undefined'; 
}

function id(d) {
	return d;
}

function curvedata() {
	var dataconcat = [],
		tsvWaiting = 0,
		ready = false,
		scopeTree = [],
		scopeId = 0,
		chartType = "line",
		rawdata,
		filter;
	
	function initFilter() {
		// init filter once all files have been added to the queue and processed
		if(ready && tsvWaiting == 0) {
			filter = createFilter();
			curvedata.setFilter(filter.dim_curve.bottom(1)[0].curve);
		}
	}
	
	function createFilter() {
		var filter = crossfilter(dataconcat);
		
		filter.dim_curve = filter.dimension(function(d) { return d.curve; });
		
		var dim_datetime = filter.dimension(function(d) { return d.date; });
		var group_date = dim_datetime.group(function(d) { return d3.time.day(d) });
		var dateMin = dim_datetime.bottom(1);
		dateMin = d3.time.day(dateMin[0].date);
		var dateMax = dim_datetime.top(1);
		dateMax = d3.time.day(dateMax[0].date);
		var selectdate = barChart()
			.dimension(dim_datetime)
			.group(group_date)
			.interval(d3.time.day)
			.x(d3.time.scale()
			.domain([dateMin, d3.time.day.offset(dateMax, 1)]).rangeRound([0, 500]));
			
		var dim_param = filter.dimension(function(d) { return d["param-size"]; });
		var group_param = dim_param.group(id);
		var paramAll = [];
		var group_all = group_param.all();
		for(var i = 0; i < group_all.length; i++) {
			paramAll.push(group_all[i].key);
		}
		var selectparam = barChart()
			.dimension(dim_param)
			.group(group_param)
			.x(d3.scale.ordinal().domain(paramAll).rangeBands([0, 500]));
			
		var dim_colors = filter.dimension(function(d) { return d.date; });
		dim_colors.grouped = dim_colors.group(id);

		
		function render(method) {
			d3.select(this).call(method);
		}
		
		filter.updateAll = function() {
			chart.each(render);
			setGraph(dim_colors, dim_param, chartType);
			if (isDef(rawdata)) {
				showdata(dim_datetime.top(Infinity));
			}
		}
		
		var chart = d3.selectAll(".filter")
			.data([selectdate, selectparam])
			.each(function(chart) { chart.on("brush", filter.updateAll).on("brushend", filter.updateAll); });
			
		filter.updateAll();
		return filter;
	}

	function showdata(data) {
		function addCols(row) {
			var rowValues = [];
			var headers = [];
			for (var key in row) {
				headers.push(key);
				rowValues.push(row[key]);
			}
			header.selectAll("th").data(headers).enter().append("th").text(id);
			d3.select(this).selectAll("td")
				.data(rowValues)
				.enter()
				.append("td").text(id);
		}
		
		var container = d3.select(rawdata);
		
		var header = container.select(".dataheader");
		if (header.empty()) {
			header = container.append("tr").attr("class", "dataheader");
		}
		
		var rows = container.selectAll(".datavalues").data(data, function(d) {
			return d.index;
		});
		
		rows.enter().append("tr").attr("class", "datavalues").each(addCols);
		rows.exit().remove();
	}
	
	function curvedata() {}

	curvedata.addScope = function(node, scope) {
		var nodeName = scope[0];
		var isLeaf = scope.length == 1;
		if (!isDef(node[nodeName])) {
			scopeId++;
			var parentDiv = isDef(node.div) ? node.div : d3.select(".tree");
			newDiv = parentDiv.append("div");
			newDiv.attr("id", "scope" + scopeId);
			if (isLeaf) {
				newDiv.append("a")
					.attr("class", "testmethod").text(nodeName)
					.attr("href", "javascript:cd.setFilter('" + scopeId +"')");
			} else {
				newDiv.text(nodeName);
			}
			node[nodeName] = {
				"id": scopeId,
				"div": newDiv
			}
		}
		if (isLeaf) {
			return scopeId;
		}	else {		
			scope.shift();
			return curvedata.addScope(node[nodeName], scope);
		}
		return scopeId;
	}
	
	curvedata.addGraph = function(scope, curveName, tsv) {
		tsvWaiting++;
		scope.push(curveName);
		var scopeId = curvedata.addScope(scopeTree, scope);
		d3.tsv(tsv, function(error, data) {
			var dateformat = d3.time.format.utc("%Y-%m-%dT%H:%M:%SZ");
			var offset = dataconcat.length;
			data.forEach(function(d, i) {
				d.curve = scopeId;
				d["param-size"] = +d["param-size"];
				d.value = +d.value;			
				d.index = offset + i;
				d.date = dateformat.parse(d.date);
			});
			dataconcat = dataconcat.concat(data);
			tsvWaiting--;
			initFilter();
		});
		
		return curvedata;
	}
	
	curvedata.setReady = function() {
		ready = true;
		initFilter();
		return curvedata;
	}
	
	curvedata.setFilter = function(curve) {
		filter.dim_curve.filter(curve);
		filter.updateAll();
	}

	curvedata.chartType = function(_) {
		if (!arguments.length) return chartType;
		chartType = _;
		filter.updateAll();
		return curvedata;
	}
	
	curvedata.rawdata = function(_) {
		if (!arguments.length) return rawdata;
		rawdata = _;
		return curvedata;
	}
	
	return curvedata;
}


var STD_MARGIN = {
	top : 20,
	right : 150,
	bottom : 30,
	left : 50
}

var STD_WIDTH = 800;
var STD_HEIGHT = 400;


/*
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
*/

function setGraph(dim_colors, dim_order, chartType) {
	d3.select(".chart").selectAll("*").remove();
	switch (chartType) {
		case "line":
			lineGraph(dim_colors, dim_order, ".chart", "value [ms]", true);
		break;
		case "bar":
			barGraph(dim_colors, dim_order, ".chart", "value [ms]");
		break;
	}
}

function barGraph(dim_groups, dim_order, node, ylabel, width, height, margin) {
	width = isDef(width) ? width : STD_WIDTH;
	height = isDef(height) ? height : STD_HEIGHT;
	margin = isDef(margin) ? margin : STD_MARGIN;

	var keyX = "param-size";
	var keyY = "value";
	
	var data = dim_groups.top(Infinity);
	var groups = dim_groups.grouped.all();
	var dateformat = d3.time.format("%Y-%m-%d %H:%M:%S");

	var w = width - margin.left - margin.right;
	var h = height - margin.top - margin.bottom;

	var x0 = d3.scale.ordinal()
			.rangeRoundBands([0, w], .1);

	var x1 = d3.scale.ordinal();

	var y = d3.scale.linear()
			.range([h, 0]);

	var xAxis = d3.svg.axis()
			.scale(x0)
			.tickFormat(dateformat)
			.orient("bottom");

	var yAxis = d3.svg.axis()
			.scale(y)
			.orient("left");

	var svg = d3.select(node).append("svg")
			.attr("width", width)
			.attr("height", height)
		.append("g")
			.attr("transform", "translate(" + margin.left + "," + margin.top + ")");

	// x0.domain([0]);
	x0.domain(groups.filter(function(d){ return (d.value != 0); }).map(function(d) { return d.key; }));
	x1.domain(dim_order.bottom(Infinity).map(function(d) { return d[keyX]; })).rangeRoundBands([0, x0.rangeBand()]);
	y.domain([0, d3.max(data, function(d) { return d[keyY]; })]);

	svg.append("g")
			.attr("class", "x axis")
			.attr("transform", "translate(0," + h + ")")
			.call(xAxis);


	svg.append("g")
			.attr("class", "y axis")
			.call(yAxis)
		.append("text")
			.attr("transform", "rotate(-90)")
			.attr("y", 6)
			.attr("dy", ".71em")
			.style("text-anchor", "end")
			.text(ylabel)
			.style("font-weight", "bold");

	var colors = d3.scale.category10();
	var data_legend = [];
	for (var i = 0; i < groups.length; i++) {
		dim_groups.filter(groups[i].key);
		data = dim_order.bottom(Infinity);
		if (data.length != 0) {
			var group = svg.selectAll(".group")
					.data([{key: 0, value: data}])
				.enter().append("g")
					.attr("class", "g")
					.attr("transform", function(d) { return "translate(" + x0(groups[i].key) + ",0)"; })
					.style("font-size", "10px");

			group.selectAll("rect")
					.data(function(d) {return d.value; })
				.enter().append("rect")
					.attr("width", x1.rangeBand())
					.attr("x", function(d) { return x1(d[keyX]); })
					.attr("y", function(d) { return y(d[keyY]); })
					.attr("height", function(d) { return h - y(d[keyY]); })
					.style("fill", function(d) { return colors(d[keyX]); });
			if (data_legend.length == 0) {
				for (var j = 0; j < data.length; j++) {
					data_legend.push({
						"text": data[j][keyX],
						"color": colors(data[j][keyX])
					});
				}
			}
		}
	}
	dim_groups.filterAll();
	legend(svg, width - margin.left, data_legend);
}


function mover(id) {
	var line = d3.selectAll(".line-" + id);
	var area = d3.select(".area-" + id);
	var legend = d3.select(".legend-" + id)

	line.transition().style("stroke-width", 4);

	area.transition().style("stroke-opacity", 1);
	area.transition().style("fill-opacity", 0.3);

	legend.select("text").attr("style", "font-weight:bold");
}

function mout(id) {
	var line = d3.selectAll(".line-" + id);
	var area = d3.select(".area-" + id);
	var legend = d3.select(".legend-" + id)

	line.transition().style("stroke-width", 1.5);

	area.transition().style("stroke-opacity", 1);
	area.transition().style("fill-opacity", 0.1);

	legend.select("text").attr("style", "font-weight:normal");
}


function lineGraph(dim_colors, dim_order, node, ylabel, showCi, width, height, margin) {
	showCi = isDef(showCi) ? showCi : false;
	width = isDef(width) ? width : STD_WIDTH;
	height = isDef(height) ? height : STD_HEIGHT;
	margin = isDef(margin) ? margin : STD_MARGIN;

	var keyX = "param-size";
	var keyY = "value";

	var cilo = "cilo";
	var cihi = "cihi";
	
	var data = dim_colors.top(Infinity);

	var w = width - margin.left - margin.right;
	var h = height - margin.top - margin.bottom;

	var x = d3.scale.linear().range([ 0, w ]);

	var y = d3.scale.linear().range([ h, 0 ]);

	var xAxis = d3.svg.axis().scale(x).orient("bottom");

	var yAxis = d3.svg.axis().scale(y).orient("left");

	var line = d3.svg.line().x(function(d) {
		return x(d[keyX]);
	}).y(function(d) {
		return y(d[keyY]);
	});

	var area = d3.svg.area().x(function(d) {
		return x(d[keyX]);
	}).y0(function(d) {
		return y(d[cilo]);
	}).y1(function(d) {
		return y(d[cihi]);
	});

	var svg = d3.select(node).append("svg").attr("width", width).attr("height", height)
		.append("g").attr("transform", "translate(" + margin.left + "," + margin.top + ")");

	x.domain(d3.extent(data, function(d) {
		return d[keyX];
	}));
	y.domain(d3.extent(data, function(d) {
		return d[keyY];
	}));

	svg.append("g").attr("class", "x axis").attr("transform",
			"translate(0," + h + ")").call(xAxis);

	svg.append("g").attr("class", "y axis").call(yAxis).append("text")
			.attr("transform", "rotate(-90)").attr("y", 6).attr("dy",
					".71em").style("text-anchor", "end").text(ylabel);

	var groups = dim_colors.grouped.all();
	var colors = d3.scale.category10();
	var data_legend = [];
	var dateformat = d3.time.format("%Y-%m-%d %H:%M:%S");
	for (var i = 0; i < groups.length; i++) {
		dim_colors.filter(groups[i].key);
		data = dim_order.bottom(Infinity);
		if (data.length != 0) {
			var color = colors(i % 10);
			svg.append("path")
				.datum(data)
				.attr("data-id", i)
				.attr("class", "line line-" + i)
				.attr("style", "stroke-opacity:0.7;stroke:" + color)
				.attr("d", line)
				.on("mouseover", function(d) { mover(d3.select(this).attr("data-id")); })
				.on("mouseout", function(d) { mout(d3.select(this).attr("data-id")); });

			svg.append("g").selectAll('circle')
				.data(data)
				.enter().append('circle')
				.attr("class", "line line-" + i)
				.attr('cx', function (d) { return x(d[keyX]); })
				.attr('cy', function (d) { return y(d[keyY]); })
				.attr('r', 5)
				.style("stroke", color);
			if (showCi) {
				svg.insert("path", ":first-child")
					.datum(data)
					.attr("class", "area-" + i)
					.attr("style", "stroke-opacity:0.2;stroke:" + color + ";fill-opacity:0.1;fill:" + color)
					.attr("d", area);
			}
			data_legend.push({
				"text": dateformat(data[0].date),
				"color": color,
				"id": i
			});
		}
	}
	dim_colors.filterAll();
	legend(svg, width - margin.left, data_legend);
}

function legend(svg, width, data) {
	var legend = svg.selectAll(".legend")
			.data(data)
		.enter().append("g")
			.attr("class", function(d) { return "legend legend-" + d.id; })
			.attr("transform", function(d, i) { return "translate(0," + i * 20 + ")"; })
			.on("mouseover", function(d) { mover(d.id); })
			.on("mouseout", function(d) { mout(d.id); });

	legend.append("rect")
			.attr("x", width - 18)
			.attr("width", 18)
			.attr("height", 18)
			.style("fill", function(d) { return d.color; } );

	legend.append("text")
			.attr("x", width - 24)
			.attr("y", 9)
			.attr("dy", ".35em")
			.text(function(d) { return d.text; });
}

/*
 * Adapted from http://square.github.com/crossfilter/
 */
function barChart() {
	if (!barChart.id) barChart.id = 0;

	var margin = {top: 10, right: 10, bottom: 20, left: 10},
		x,
		y = d3.scale.linear().range([100, 0]),
		id = barChart.id++,
		axis = d3.svg.axis().orient("bottom"),
		brush = d3.svg.brush(),
		interval,
		brushDirty,
		dimension,
		group,
		round;

	function chart(div) {
		var width = 500;//TODO x.range()[1],
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
		/*
		firstKey = x.domain()[0],
		secondKey = interval.offset(firstKey, 1),
		barWidth = 0.9 * x(secondKey) - x(firstKey);
		console.log(barWidth);
		*/
	while (++i < n) {
				d = groups[i];
		barWidth = isDef(interval) ? 0.9 * (x(interval.offset(d.key, 1)) - x(d.key)) : 100; //TODO
				path.push("M", x(d.key), ",", height, "V", y(d.value), "h", barWidth, "V", height);
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
		if (isDef(x.rangeBand)) {
		function invertScale(xPixels) {
			var i = Math.round(xPixels / x.rangeBand());
		if (i < group.size()) {
			return group.all()[i].key;
		} else {
			return group.all()[i-1].key+1;
		}
		//return group.all()[Math.min(, group.size()-1)].key;
		}
		invExtent = [invertScale(extent[0]), invertScale(extent[1])];
		/*
		g.select(".brush")
			.call(brush.extent(extent = [x(invExtent[0]), x(invExtent[1])]))
			.selectAll(".resize")
			.style("display", null);*/
		g.select("#clip-" + id + " rect")
			.attr("x", extent[0])
			.attr("width", extent[1] - extent[0]);
		dimension.filterRange(invExtent);
	} else {
		if (round) g.select(".brush")
			.call(brush.extent(extent = extent.map(round)))
			.selectAll(".resize")
			.style("display", null);
		g.select("#clip-" + id + " rect")
			.attr("x", x(extent[0]))
			.attr("width", x(extent[1]) - x(extent[0]));
		dimension.filterRange(extent);
	}
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
	console.log(x.domain());
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

	chart.interval = function(_) {
		if (!arguments.length) return interval;
	interval = _;
		round = interval.round;
		return chart;
	};

	return d3.rebind(chart, brush, "on");
}