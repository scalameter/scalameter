var GenericChart = function(helper) {
	var my = {};

	/*
	 * ----- imports -----
	 */
	var h = helper,
		dKey = h.dKey,
		mapKey = h.mapKey,
		ident = h.ident
		unique = h.unique;

	/*
	 * ----- constants -----
	 */

	var STD_MARGIN = {
		top : 20,
		right : 20,
		bottom : 30,
		left : 50
	}

	var STD_WIDTH = 800;
	var STD_HEIGHT = 400;
	var STD_LEGEND_WIDTH = 200;

	var CHART_TYPES = {
		lineParam: { value: "lineParam", name: "Line Chart (param)" },
		lineDate: { value: "lineDate", name: "Line Chart (date)" },
		bar: { value: "bar", name: "Bar Chart" }
/*		line: { value: "line", name: "Line Chart" },
		bar: { value: "bar", name: "Bar Chart" }*/
	};

	/*
	 * ----- private properties -----
	 */
	var
		svg_ = null,
		width_ = STD_WIDTH,
		height_ = STD_HEIGHT,
		margin_ = STD_MARGIN,
		legendWidth_ = STD_LEGEND_WIDTH,
		showCI_ = false,
		chartType_ = CHART_TYPES.lineParam;

	/*
	 * ----- public properties -----
	 */
	my.cType = CHART_TYPES;

	my.setWidth = function(_) {
		width_ = _;
		return my;
	};

	my.setHeight = function(_) {
		height_ = _;
		return my;
	};

	my.setMargin = function(_) {
		margin_ = _;
		return my;
	};

	my.setType = function(_) {
		chartType_ = _;
		return my;
	};

	my.setShowCI = function(_) {
		showCI_ = _;
		return my;
	};

	function sortBy(fn) {
		return function(a, b) {
			return d3.ascending(fn(a), fn(b));
		};
	}

	function msToDateStr(d) {
		return d3.time.format("%Y-%m-%d %H:%M:%S")(new Date(+d));
	}

	function flashBars(id) {
		/*
		TODO
		var bars = d3.selectAll(".bar-" + id);
		if (!bars.empty()) {
			var width = bars.attr("width");
			bars
				.transition()
				.attr("width", 1.1 * width)
				.transition()
				.attr("width", width);
			setTimeout(function() { flashBars(id); }, 600);
		}
		*/
	}

	function mover(id) {
		var line = d3.selectAll(".line-" + id);
		var area = d3.select(".area-" + id);
		var legend = d3.select(".legend-" + id)

		line.transition().style("stroke-width", 4);

		area.transition().style("stroke-opacity", 1);
		area.transition().style("fill-opacity", 0.3);

		legend.select("text").attr("style", "font-weight:bold");

		flashBars(id);
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

	function createColorMap(mainColors, domain_shades) {
		var brightness = d3.scale.ordinal()
			.domain(domain_shades)
			.rangePoints([-1, 1]);
		return (function(i, j) {
			return d3.hsl(mainColors(i)).brighter(brightness(j));
		});
	}
/*
	function chartFactory(data) {
		var my = {};

		var TYPES = {
			line: { value: "line", name: "Line Chart",  },
			bar: { value: "bar", name: "Bar Chart" }
		};


		var param = {
			lineData: [],
			barData: [],

		}

		function bar() {}

		return my;
	};

	}
*/
	my.update = function(data, node, ylabel, mainColors) {
		var W = width_ - margin_.left - margin_.right - legendWidth_;
		var H = height_ - margin_.top - margin_.bottom;

		var keyCurve = {
			get: mapKey(dKey.curve),
			sort: d3.ascending
		};

		var keyAbscissa,
				keyLegend,
				showXGrid = true,
				xLabelFormat = ident,
				lineData = [],
				barData = [];


		// scales
		var x,
				y = d3.scale.linear()
			.domain([0, d3.max(data, mapKey(dKey.value))])
			.range([H, 0]);

		switch(chartType_) {
			case CHART_TYPES.lineParam:
				lineData = data;
				keyAbscissa = mapKey(dKey.param);

				keyLegend = {
					get: mapKey(dKey.date),
					sort: d3.descending,
					format: msToDateStr
				};

				x = d3.scale.linear()
							.domain(d3.extent(data, keyAbscissa))
							.range([0, W]);
				break;
			case CHART_TYPES.lineDate:
				lineData = data;
				keyAbscissa = mapKey(dKey.date);

				keyLegend = {
					get: mapKey(dKey.param),
					sort: d3.descending,
					format: ident
				};

				var keys_abscissa = unique(data, keyAbscissa, d3.ascending);
				x = d3.scale.ordinal()
							.domain(keys_abscissa)
							.rangePoints([0, W]);

				xLabelFormat = msToDateStr;
				break;
			case CHART_TYPES.bar:
				barData = data;
				showXGrid = false;

				keyLegend = {
					get: mapKey(dKey.param),
					sort: d3.ascending,
					format: ident
				};

				keyAbscissa0 = mapKey(dKey.date);
				keyAbscissa1 = mapKey(dKey.param);
				keyAbscissa2 = mapKey(dKey.curve);

				var x = d3.scale.ordinal()
										.domain(unique(data, keyAbscissa0, d3.ascending))
										.rangeRoundBands([0, W], 0.1);

				var x1 = d3.scale.ordinal()
										.domain(unique(data, keyAbscissa1, d3.ascending))
										.rangeRoundBands([0, x.rangeBand()], 0.1);

				var x2 = d3.scale.ordinal()
										.domain(unique(data, keyAbscissa2, d3.ascending))
										.rangeRoundBands([0, x1.rangeBand()]);

				xLabelFormat = msToDateStr;
				break;
		}

		// group by keyCurve, keyLegend
		var group_outer = d3.nest()
			.key(keyCurve.get).sortKeys(keyCurve.sort)
			.key(keyLegend.get).sortKeys(keyLegend.sort)
			.sortValues(sortBy(keyAbscissa));
		var data_outer = group_outer.entries(lineData);

		var keys_outer = data_outer.map(h.fKey);

		var keysCurveColor = unique(data, keyCurve.get, keyCurve.sort);

		var keysGradient = unique(data, keyLegend.get, keyLegend.sort);

		var colorMap = createColorMap(mainColors, keysGradient);

		if (svg_ === null) {
			svg_ = d3.select(node).append("svg").attr("width", width_).attr("height", height_)
				.append("g").attr("transform", "translate(" + margin_.left + "," + margin_.top + ")");

			// grid
			svg_.append("g")         
	        .attr("class", "x grid")
	        .attr("transform", "translate(0," + H + ")");
			svg_.append("g")         
	        .attr("class", "y grid");

			// x axis
			svg_.append("g")
				.attr("class", "x axis")
				.attr("transform", "translate(0," + H + ")");

			// y axis
			svg_.append("g")
				.attr("class", "y axis")
				.append("text")
					.attr("transform", "rotate(-90)")
					.attr("y", 6).attr("dy", ".71em")
					.style("text-anchor", "end")
					.text(ylabel);
		}

		var path = function(d) {
			var line = d3.svg.line().x(function(d) {
				return x(keyAbscissa(d));
			}).y(function(d) {
				return y(d[dKey.value]);
			});

			var g = d3.select(this);
			var cls = "line line-" +d.key;
			var color = colorMap(keyCurve.get(d.values[0]), d.key);
			g.select("path")
				.attr("class", cls)
				.attr("style", "stroke-opacity:0.7;stroke:" + color)
				.on("mouseover", function(d) { mover(d.key); })
				.on("mouseout", function(d) { mout(d.key); })
				.transition()
				.attr("d", line(d.values));
		}

		var area_ci = function(d) {
			var area = d3.svg.area().x(function(d) {
				return x(keyAbscissa(d));
			}).y0(function(d) {
				return y(d[dKey.cilo]);
			}).y1(function(d) {
				return y(d[dKey.cihi]);
			});

			var g = d3.select(this);
			var color = colorMap(keyCurve.get(d.values[0]), d.key);
			g.select("path")
				.attr("class", "area-" + d.key)
				.attr("style", "stroke-opacity:0.2;stroke:" + color + ";fill-opacity:0.1;fill:" + color)
				.transition()
				.attr("d", area(d.values));
		}

		function curveFn(fn) {
			return function(d) {
				var groups = d3.select(this).selectAll(".group").data(d.values, h.fKey);

				groups.enter()
					.append("g")
					.attr("class", "group")
					.append("path");

				groups.each(fn);

				groups.exit().remove();
			}
		}

		function bar(d) {
			d3.select(this)
				.style("fill", colorMap(keyCurve.get(d), keyAbscissa1(d)))
				.transition()
				.attr("width", x2.rangeBand())
				.attr("x", x(keyAbscissa0(d)) + x1(keyAbscissa1(d)) + x2(keyAbscissa2(d)))
				.attr("y", y(d[dKey.value]))
				.attr("height", H - y(d[dKey.value]))
				.transition()
				.style("fill-opacity", 1);
		}

		function legendRow(d) {
			var g = d3.select(this);

			g.select("text")
				.attr("x", -4)
				.attr("y", 9)
				.attr("dy", ".35em")
				.text(keyLegend.format(d));

			var rects = g.selectAll("rect").data(keysCurveColor, ident);
			rects.enter().append("rect")
				.attr("width", 18)
				.attr("height", 18)
				.style("fill", function(d0) { return colorMap(d0, d); } );
			rects.attr("x", function(d, i) { return 20 * i + 2; } );
			rects.exit().remove();
		}

		d3.transition().duration(2000).each(function() {
			// axis and grid
			if (showXGrid) {
				var xGrid = d3.svg.axis().scale(x).orient("bottom").tickSize(-H, 0, 0).tickFormat("");
				svg_.select(".x.grid").transition().call(xGrid);
			} else {
				svg_.select(".x.grid").selectAll("*").remove();
			}

			var yGrid = d3.svg.axis().scale(y).orient("left").tickSize(-W, 0, 0).tickFormat("");
			svg_.select(".y.grid").transition().call(yGrid);

			var xAxis = d3.svg.axis().scale(x).orient("bottom").tickFormat(xLabelFormat);
			svg_.select(".x.axis").transition().call(xAxis);

			var yAxis = d3.svg.axis().scale(y).orient("left");
			svg_.select(".y.axis").transition().call(yAxis);

			var bars = svg_.selectAll("rect").data(barData, mapKey(dKey.index));

			bars.enter()
				.append("rect")
				.attr("class", function(d) { return "bar-" + keyAbscissa1(d); })
				.style("fill-opacity", 0);

			bars.each(bar);

			bars.exit().remove();

			// data points
			var points = svg_.selectAll('circle').data(lineData, mapKey(dKey.index));

			points.enter().append('circle')
				.attr("class", function(d) { return "line line-" + keyLegend.get(d); })
				.attr('r', 5);

			points
				.style("stroke",  function(d) { return colorMap(keyCurve.get(d), keyLegend.get(d)); })
				.transition()
				.attr('cx', function (d) { return x(keyAbscissa(d)); })
				.attr('cy', function (d) { return y(d[dKey.value]); });

			points.exit().remove();

			// data paths
			var curves = svg_.selectAll(".curve").data(data_outer, h.fKey);

			curves.enter()
				.append("g")
				.attr("class", "curve");
			
			curves.each(curveFn(path));

			curves.exit().remove();

			// confidence intervals (areas)
			var curves_ci = svg_.selectAll(".curve_ci").data(showCI_ ? data_outer : [], h.fKey);

			curves_ci.enter()
				.insert("g", ":first-child")
				.attr("class", "curve_ci");
			
			curves_ci.each(curveFn(area_ci));

			curves_ci.exit().remove();

			// legend
			var legend = svg_.selectAll(".legend").data(keysGradient, ident);

			legend.enter()
				.append("g")
				.attr("class", function(d) { return "legend legend-" + d; })
				.on("mouseover", mover)
				.on("mouseout", mout)
				.append("text");
			
			legend
				.attr("transform", function(d, i) { return "translate(" + (W + legendWidth_ - 20 * keysCurveColor.length) + ", " + i * 20 + ")"; })
				.each(legendRow);

			legend.exit().remove();
		});
	}

	return my;
};