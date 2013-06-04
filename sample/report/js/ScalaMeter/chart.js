var ScalaMeter = (function(parent) {
	var my = { name: "chart" };

	/*
	 * ----- imports -----
	 */
	var h = parent.helper,
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
		/*lineParam: { value: "lineParam", name: "Line Chart (param)" },
		lineDate: { value: "lineDate", name: "Line Chart (date)" },
		bar: { value: "bar", name: "Bar Chart" }*/
		line: { value: "line", name: "Line Chart" },
		bar: { value: "bar", name: "Bar Chart" }
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
		chartType_ = CHART_TYPES.line;

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

	my.toggleCI = function() {
		showCI_ = !showCI_;
		d3.select(".showci").classed("label-info", showCI_);
		return my;
	}

	// function msToDateStr(d) {
	// 	return d3.time.format("%Y-%m-%d %H:%M:%S")(new Date(+d));
	// }

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

	//TODO
	// function createColorMap(domain_shades) {
	// 	var brightness = d3.scale.ordinal()
	// 		.domain(domain_shades)
	// 		.rangePoints([-1, 1]);
	// 	return (function(i, j) {
	// 		return d3.hsl(h.mainColors(i)).brighter(brightness(j));
	// 	});
	// }

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
	my.update = function(data, node, ylabel, filterDimensions) {
		var W = width_ - margin_.left - margin_.right - legendWidth_;
		var H = height_ - margin_.top - margin_.bottom;

		var getCurveKey = mapKey(dKey.curve);
		// var keyCurve = {
		// 	get: mapKey(dKey.curve),
		// 	sort: d3.ascending
		// };

		var keyAbscissa,
				keyLegend,
				showXGrid = true,
				xLabelFormat = ident,
				legendOrder,
				lineData = [],
				groupedLineData = [],
				barData = [];


		// scales
		var x,
			y = d3.scale.linear()
				.domain([0, d3.max(data, mapKey(dKey.value))])
				.range([H, 0]);

		var legendDimensions = filterDimensions.slice(1); //TODO

		var legendData = [];
		var legendSize = 1;

		function createColorMap2() {
			var brightness = d3.scale.linear()
				.domain([0, legendSize - 1])
				.range([-1, 1]);
			return function(d) {
				var index = 0;
				legendData.forEach(function(legendDim) {
					index = index * legendDim.values.length + legendDim.values.indexOf(legendDim.key(d));
				});
				return d3.hsl(h.mainColors(getCurveKey(d))).brighter(brightness(index));
			};
		}


		//TODO handle null values
		switch(chartType_) {
			case CHART_TYPES.line:
				lineData = data;
				keyAbscissa = filterDimensions[0].keyFn();

				keyLegend = {
					get: mapKey(dKey.date),
					sort: d3.descending
					// format: msToDateStr
				};
				legendOrder = d3.descending;

				x = d3.scale.linear()
							.domain(d3.extent(data, keyAbscissa))
							.range([0, W]);

				// group by keyCurve, keyLegend
				// var nestLineData = d3.nest()
				// 	.key(getCurveKey) //.sortKeys(keyCurve.sort)
				// 	.key(keyLegend.get) //.sortKeys(keyLegend.sort)
				// 	.sortValues(h.sortBy(keyAbscissa));

				var nestLineData = d3.nest().key(getCurveKey);
				legendDimensions.forEach(function(dim) {
					nestLineData.key(dim.keyFn());
				});
				nestLineData.sortValues(h.sortBy(keyAbscissa));

				groupedLineData = nestLineData.entries(lineData);


				//TODO xLabelFormat
				break;
				/*
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
				*/
			case CHART_TYPES.bar:
				barData = data;
				showXGrid = false;

				keyLegend = {
					get: legendDimensions[0].keyFn(),
					sort: d3.ascending,
					format: ident
				};
				legendOrder = d3.ascending;

				var xBar = function() { return 0; };
				var xN = [];

				var parentWidth = W;
				var keyFuns = filterDimensions.map(function(dim) {
					return dim.keyFn();
				});
				keyFuns.push(getCurveKey);

				for (var i = 0; i < keyFuns.length; i++) {
					xN.push(
						d3.scale.ordinal()
							.domain(unique(data, keyFuns[i], d3.ascending))
							.rangeRoundBands([0, parentWidth], i == keyFuns.length - 1 ? 0 : 0.1)
					);
					parentWidth = xN[i].rangeBand();
					var xBar = (function(xBarPrev, xN, keyFn) {
						return function(d) {
							return xBarPrev(d) + xN(keyFn(d));
						};
					})(xBar, xN[i], keyFuns[i]);
				}
				var barWidth = parentWidth;
				x = xN[0];

				//TODO xLabelFormat = msToDateStr;
				break;
		}

		legendDimensions.forEach(function(dim) {
			var key = dim.keyFn();
			var values = unique(data, key, legendOrder);
			legendSize *= values.length;
			legendData.push({
				key: key,
				values: values,
				format: dim.format()
			});
		});




		// var keys_outer = groupedLineData.map(h.fKey);

		var keysCurveColor = unique(data, getCurveKey, d3.ascending);

		var keysGradient = unique(data, keyLegend.get, keyLegend.sort);


		// var colorMap = createColorMap(keysGradient);
		var colorMap2 = createColorMap2();

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

			// legend
			svg_.append("g")
				.attr("class", "legend");
		}

		var path = function(d) {
			console.log("PATH"); console.log(d);
			var line = d3.svg.line().x(function(d) {
				return x(keyAbscissa(d));
			}).y(function(d) {
				return y(d[dKey.value]);
			});

			var g = d3.select(this);
			console.log(g);
			var cls = "line line-" +d.key;
			// var color = colorMap(getCurveKey(d.values[0]), d.key);
			var color = colorMap2(d.values[0]);
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
			var color = colorMap2(d.values[0]);
			g.select("path")
				.attr("class", "area-" + d.key)
				.attr("style", "stroke-opacity:0.2;stroke:" + color + ";fill-opacity:0.1;fill:" + color)
				.transition()
				.attr("d", area(d.values));
		}

		function curveFn(fn, depth) {
			return function(d) {
				var groups = d3.select(this).selectAll(".group-" + depth).data(d.values, h.fKey);

				var subGroups = groups.enter()
					.append("g")
					.attr("class", "group-" + depth);

				if (depth < legendDimensions.length) {
					groups.each(curveFn(fn, depth + 1));
				} else {
					subGroups
						.append("path");

					groups.each(fn);
				}
				groups.exit().remove();
			}
		}

		function bar(d) {
			d3.select(this)
				// .style("fill", colorMap(getCurveKey(d), keyLegend.get(d)))
				.style("fill", colorMap2(d))
				.transition()
				//.attr("width", x2.rangeBand())
				.attr("width", barWidth)
				.attr("x", xBar(d))
				// .attr("x", x0(keyAbscissa0(d)) + x1(keyAbscissa1(d)) + x2(keyAbscissa2(d)))
				.attr("y", y(d[dKey.value]))
				.attr("height", H - y(d[dKey.value]))
				.transition()
				.style("fill-opacity", 1);
		}

		function legendRow(d) {
			var g = d3.select(this);

/*
			g.select("text")
				.attr("x", -4)
				.attr("y", 9)
				.attr("dy", ".35em")
				.text(keyLegend.format(d));

			var rects = g.selectAll("rect").data(keysCurveColor, ident);
			rects.enter().append("rect")
				.attr("width", 18)
				.attr("height", 18)
				.style("fill", function(d0) { return colorMap2(d); } );
			rects.attr("x", function(d, i) { return 20 * i + 2; } );
			rects.exit().remove();
			*/
		}

		function legendGroup(depth, from, groupSize) {
			return function(d, i) {
				console.log("DDD");
				var groups = d3.select(this).selectAll(".legend-grp-" + depth).data(legendData[depth].values, h.ident);
				// var groupSize = legendSize / legendData[depth].branches;
				var subGroupSize = groupSize / legendData[depth].values.length;
				console.log(subGroupSize);

				groups.enter()
					.append("g")
					.attr("class", "legend-grp-" + depth)
					.append("text")
					.text(legendData[depth].format);

				if (depth < legendData.length - 1) {
					groups
						.attr("transform", function(d, i) { return "translate(0, " + ((1 + i * (subGroupSize + 1)) * 20) + ")"; })
						.each(legendGroup(depth + 1, from + i * subGroupSize, subGroupSize));
				} else {
					groups
						.attr("transform", function(d, i) { return "translate(0, " + ((1 + i * subGroupSize) * 20) + ")"; })
						.each(legendRow);
				}
				groups.exit().remove();
			};
		}

		d3.transition()./*duration(2000).*/each(function() {
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
				//TODO .attr("class", function(d) { return "bar-" + keyAbscissa1(d); })
				.style("fill-opacity", 0);

			bars.each(bar);

			bars.exit().remove();

			// data points
			var points = svg_.selectAll('circle').data(lineData, mapKey(dKey.index));

			points.enter().append('circle')
				.attr("class", function(d) { return "line line-" + keyLegend.get(d); })
				.attr('r', 5);

			points
				.style("stroke",  function(d) { return colorMap2(d); })
				.transition()
				.attr('cx', function (d) { return x(keyAbscissa(d)); })
				.attr('cy', function (d) { return y(d[dKey.value]); });

			points.exit().remove();

			// data paths
			var curves = svg_.selectAll(".curve").data(groupedLineData, h.fKey);

			curves.enter()
				.append("g")
				.attr("class", "curve");
			
			curves.each(curveFn(path, 1));

			curves.exit().remove();

			// confidence intervals (areas)
			var curves_ci = svg_.selectAll(".curve_ci").data(showCI_ ? groupedLineData : [], h.fKey);

			curves_ci.enter()
				.insert("g", ":first-child")
				.attr("class", "curve_ci");
			
			curves_ci.each(curveFn(area_ci, 1));

			curves_ci.exit().remove();

			// legend
			//TODO appendifnotexists
			svg_.select(".legend")
				.attr("transform", "translate(" + (W + legendWidth_ - 20 * keysCurveColor.length) + ", 0)")
				.each(legendGroup(0, 0, legendSize));
			// var legend = svg_.selectAll(".legend").data(keysGradient, ident);

			// legend.enter()
			// 	.append("g")
			// 	.attr("class", function(d) { return "legend legend-" + d; })
			// 	.on("mouseover", mover)
			// 	.on("mouseout", mout)
			// 	.append("text");
			
			// legend
			// 	.attr("transform", function(d, i) { return "translate(" + (W + legendWidth_ - 20 * keysCurveColor.length) + ", " + i * 20 + ")"; })
			// 	.each(legendRow);

			// legend.exit().remove();
		});
	}

	parent[my.name] = my;

	return parent;
})(ScalaMeter || {});