function isDef(value) {
	return typeof value !== 'undefined'; 
}

function ident(d) {
	return d;
}

var curvedata = (function() {
	var my = {},
		dataconcat = [],
		tsvWaiting = 0,
		ready = false,
		scopeTree = { children: [] },
		scopeId = 0,
		rawdata,
		filter,
		svg = null,
		dKey = {
			date: "date",
			param: "param-size",
			value: "value",
			success: "success",
			cilo: "cilo",
			cihi: "cihi",
			complete: "complete",
			curve: "curve",
			index: "index",
			get: function(key) {
				return function(d) { return d[key] };
			}
		},
		tChart = {
			lineParam: { value: "lineParam", name: "Line Chart (param)" },
			lineDate: { value: "lineDate", name: "Line Chart (date)" },
			bar: { value: "bar", name: "Bar Chart" }
		},
		chartType = tChart.lineParam,
		showCI = false;


	function fnKey(d) {
		return d.key;
	}

	function sortBy(fn) {
		return function(a, b) {
			return d3.ascending(fn(a), fn(b));
		};
	}

	function unique(data, key, sort){
		var values = d3.nest().key(key).map(data, d3.map).keys();
		if (isDef(sort)) {
			return values.sort(sort);
		} else {
			return values;
		}
	}
	
	function init() {
		// init filter once all files have been added to the queue and processed
		if(ready && tsvWaiting == 0) {
			filter = createFilter();
			initTree();
		}
	}
	
	function createFilter() {
		var filter = crossfilter(dataconcat);
		
		filter.dim_curve = filter.dimension(function(d) { return d[dKey.curve]; });
		
		var dim_datetime = filter.dimension(function(d) { return d[dKey.date]; });
		var group_date = dim_datetime.group(function(d) { return d3.time.day(new Date(d)) });
		var dateMin = dim_datetime.bottom(1);
		dateMin = d3.time.day(new Date(dateMin[0][dKey.date]));
		var dateMax = dim_datetime.top(1);
		dateMax = d3.time.day(new Date(dateMax[0][dKey.date]));
		var selectdate = barChart()
			.dimension(dim_datetime)
			.group(group_date)
			.interval(d3.time.day)
			.x(d3.time.scale()
			.domain([dateMin, d3.time.day.offset(dateMax, 1)]).rangeRound([0, 500]));
			
		var dim_param = filter.dimension(function(d) { return d[dKey.param]; });
		var group_param = dim_param.group(ident);
		var paramAll = [];
		var group_all = group_param.all();
		for(var i = 0; i < group_all.length; i++) {
			paramAll.push(group_all[i].key);
		}
		var selectparam = barChart()
			.dimension(dim_param)
			.group(group_param)
			.x(d3.scale.ordinal().domain(paramAll).rangeBands([0, 500]));
			
		var dim_colors = filter.dimension(function(d) { return d[dKey.date]; });
		dim_colors.grouped = dim_colors.group(ident);

		
		function render(method) {
			d3.select(this).call(method);
		}
		
		filter.updateAll = function() {
			chart.each(render);
			// d3.select(".chart").selectAll("*").remove();
			if (chartType == "bar") {
				barGraph(dim_colors, dim_param, ".chart", "value [ms]");
			} else {
				updateChart(dim_colors, chartType, ".chart", "value [ms]", showCI);
			}
			if (isDef(rawdata)) {
				showdata(dim_datetime.top(Infinity));
			}
		}
		
		var chart = d3.selectAll(".filter")
			.data([selectdate, selectparam])
			.each(function(chart) { chart.on("brush", filter.updateAll).on("brushend", filter.updateAll); });
			
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
			header.selectAll("th").data(headers).enter().append("th").text(ident);
			d3.select(this).selectAll("td")
				.data(rowValues)
				.enter()
				.append("td").text(ident);
		}
		
		var container = d3.select(rawdata);
		
		var header = container.select(".dataheader");
		if (header.empty()) {
			header = container.append("tr").attr("class", "dataheader");
		}
		
		var rows = container.selectAll(".datavalues").data(data, dKey.get(dKey.index));
		
		rows.enter().append("tr").attr("class", "datavalues").each(addCols);
		rows.exit().remove();
	}

	/*
	 * Initialize the dynatree widget, using the
	 * data accumulated in scopeTree.
	 */
	function initTree() {
		function convertTree(node, title) {
			var children = [];
			for (child in node.children) {
				children.push(
					convertTree(node.children[child], child)
				);
			}
			if (title != "") {
				title = '<span class="dynatree-adjtext">' + title + '</span>';
				if (node.id != -1) {
					var color = mainColors(node.id);
					title = '<div class="dynatree-square" style="background-color:' + color + '"></div>' + title;
				}
				return {
					key: "" + node.id,
					title: title,
					expand: true,
					children: children
				}
			} else {
				return children;
			}
		}
		var children = convertTree(scopeTree, "");
		$(".tree").dynatree({
			onSelect: function(flag, node) {
				var selectedNodes = node.tree.getSelectedNodes();
				var selectedKeys = $.map(selectedNodes, function(node){
					return +node.data.key;
				});
				setFilter(selectedKeys);
			},
			onQueryActivate: function(flag, node) {
				node.toggleSelect();
				return false;
			},
			children: children,
			checkbox: true, // Show checkboxes.
			clickFolderMode: 1, // 1:activate, 2:expand, 3:activate and expand
			selectMode: 3, // 1:single, 2:multi, 3:multi-hier
			noLink: true, // Use <span> instead of <a> tags for all nodes,
			classNames: {
				nodeIcon: "none"
			}
		});
		// Select first leaf node
		$(".tree").dynatree("getTree").selectKey("0");
	}
	
	function addScope(node, scope) {
		var nodeName = scope[0];
		var isLeaf = scope.length == 1;
		var id = isLeaf ? scopeId : -1;
		if (!isDef(node.children[nodeName])) {
			node.children[nodeName] = {
				"id": id,
				"children": []
			}
		}
		if (isLeaf) {
			scopeId++;
			return id;
		}	else {		
			scope.shift();
			return addScope(node.children[nodeName], scope);
		}
	}


	var STD_MARGIN = {
		top : 20,
		right : 20,
		bottom : 30,
		left : 50
	}

	var STD_WIDTH = 800;
	var STD_HEIGHT = 400;
	var STD_LEGEND_WIDTH = 200;





	function barGraph(dim_groups, dim_order, node, ylabel, width, height, margin) {
		function legend(svg, x, data) {
			var legend = svg.selectAll(".legend")
					.data(data)
				.enter().append("g")
					.attr("class", function(d) { return "legend legend-" + d.id; })
					.attr("transform", function(d, i) { return "translate(0," + i * 20 + ")"; })
					.on("mouseover", function(d) { mover(d.id); })
					.on("mouseout", function(d) { mout(d.id); });

			legend.append("rect")
					.attr("x", x - 18)
					.attr("width", 18)
					.attr("height", 18)
					.style("fill", function(d) { return d.color; } );

			legend.append("text")
					.attr("x", x - 24)
					.attr("y", 9)
					.attr("dy", ".35em")
					.text(function(d) { return d.text; });
		}

		width = isDef(width) ? width : STD_WIDTH;
		height = isDef(height) ? height : STD_HEIGHT;
		margin = isDef(margin) ? margin : STD_MARGIN;

		var keyX = "param-size";
		var keyY = "value";
		
		var data = dim_groups.top(Infinity);
		var groups = dim_groups.grouped.all();
		var dateformat = function(d) { return d3.time.format("%Y-%m-%d %H:%M:%S")(new Date(d)) };

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

	var mainColors = (function() {
		var nGroups = 10;
		var groups = d3.scale.category10().domain(d3.range(nGroups));
		return function(i) {
			return groups(i % nGroups);
		};
	})();

	function createColorMap(domain_shades) {
		var brightness = d3.scale.ordinal()
			.domain(domain_shades)
			.rangePoints([-1, 1]);
		return (function(i, j) {
			return d3.hsl(mainColors(i)).brighter(brightness(j));
		});
	}

	function updateChart(dim_colors, chartType, node, ylabel, showCi, width, height, margin, legend_width) {
		width = isDef(width) ? width : STD_WIDTH;
		height = isDef(height) ? height : STD_HEIGHT;
		margin = isDef(margin) ? margin : STD_MARGIN;
		legend_width = isDef(legend_width) ? legend_width : STD_LEGEND_WIDTH;

		var w = width - margin.left - margin.right - legend_width;
		var h = height - margin.top - margin.bottom;

		var data = dim_colors.top(Infinity);

		var keyCurve = {
			get: dKey.get(dKey.curve),
			sort: d3.ascending
		};

		var keyAbscissa,
				keyLegend,
				isBar = false;

		// scales
		var x,
				y = d3.scale.linear()
			.domain([0, d3.max(data, dKey.get(dKey.value))])
			.range([h, 0]);

		switch(chartType) {
			case tChart.lineParam:
				keyAbscissa = dKey.get(dKey.param);

				keyLegend = {
					get: dKey.get(dKey.date),
					sort: d3.descending,
					format: function(d) { return d3.time.format("%Y-%m-%d %H:%M:%S")(new Date(+d)) }
				};

				x = d3.scale.linear()
							.domain(d3.extent(data, keyAbscissa))
							.range([0, w]);
				break;
			case tChart.lineDate:
				keyAbscissa = dKey.get(dKey.date);

				keyLegend = {
					get: dKey.get(dKey.param),
					sort: d3.descending,
					format: ident
				};

				var keys_abscissa = unique(data, keyAbscissa, d3.ascending);
				x = d3.scale.ordinal()
							.domain(keys_abscissa)
							.rangePoints([0, w]);
				break;
			case tChart.bar:
				isBar = true;

				keyLegend = {
					get: dKey.get(dKey.param),
					sort: d3.ascending,
					format: ident
				};

				keyAbscissa0 = dKey.get(dKey.date);
				keyAbscissa1 = dKey.get(dKey.param);
				keyAbscissa2 = dKey.get(dKey.curve);

				var x = d3.scale.ordinal()
										.domain(unique(data, keyAbscissa0, d3.ascending))
										.rangeRoundBands([0, w], 0.1);

				var x1 = d3.scale.ordinal()
										.domain(unique(data, keyAbscissa1, d3.ascending))
										.rangeRoundBands([0, x.rangeBand()], 0.1);

				var x2 = d3.scale.ordinal()
										.domain(unique(data, keyAbscissa2, d3.ascending))
										.rangeRoundBands([0, x1.rangeBand()]);
		}

		if (!isBar) {
			// group by keyCurve, keyLegend
			var group_outer = d3.nest()
				.key(keyCurve.get).sortKeys(keyCurve.sort)
				.key(keyLegend.get).sortKeys(keyLegend.sort)
				.sortValues(sortBy(keyAbscissa));
			var data_outer = group_outer.entries(data);

			var keys_outer = data_outer.map(fnKey);
		}

		var keys_inner = unique(data, keyLegend.get, keyLegend.sort);

		var colorMap = createColorMap(keys_inner);

		if (svg === null) {
			svg = d3.select(node).append("svg").attr("width", width).attr("height", height)
				.append("g").attr("transform", "translate(" + margin.left + "," + margin.top + ")");

			// grid
			svg.append("g")         
	        .attr("class", "x grid")
	        .attr("transform", "translate(0," + h + ")");
			svg.append("g")         
	        .attr("class", "y grid");

			// x axis
			svg.append("g")
				.attr("class", "x axis")
				.attr("transform", "translate(0," + h + ")");

			// y axis
			svg.append("g")
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
			
			// var datapoints = g.selectAll('circle').data(d.values, keyAbscissa);

			// datapoints.enter().append('circle')
			// 	.attr("class", cls)
			// 	.attr('r', 5)
			// 	.style("stroke", color);

			// datapoints.transition()
			// 	.attr('cx', function (d) { return x(keyAbscissa(d)); })
			// 	.attr('cy', function (d) { return y(d[dKey.value]); });

			// datapoints.exit().remove();
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
				var groups = d3.select(this).selectAll(".group").data(d.values, fnKey);

				groups.enter()
					.append("g")
					.attr("class", "group")
					.append("path");

				groups.each(fn);

				groups.exit().remove();
			}
		}

		function legendRow(d) {
			var g = d3.select(this);

			g.select("text")
				.attr("x", -4)
				.attr("y", 9)
				.attr("dy", ".35em")
				.text(keyLegend.format(d));

			var rects = g.selectAll("rect").data(keys_outer, ident);
			rects.enter().append("rect")
				.attr("width", 18)
				.attr("height", 18)
				.style("fill", function(d0) { return colorMap(d0, d); } );
			rects.attr("x", function(d, i) { return 20 * i + 2; } );
			rects.exit().remove();
		}

		d3.transition()/*.duration(2000)*/.each(function() {
			// axis and grid
			if (!isBar) {
				var xGrid = d3.svg.axis().scale(x).orient("bottom").tickSize(-h, 0, 0).tickFormat("");
				svg.select(".x.grid").transition().call(xGrid);
			}

			var yGrid = d3.svg.axis().scale(y).orient("left").tickSize(-w, 0, 0).tickFormat("");
			svg.select(".y.grid").transition().call(yGrid);

			var xAxis = d3.svg.axis().scale(x).orient("bottom");
			svg.select(".x.axis").transition().call(xAxis);

			var yAxis = d3.svg.axis().scale(y).orient("left");
			svg.select(".y.axis").transition().call(yAxis);

			if (isBar) {
				var bars = svg.selectAll("rect").data(data, dKey.get(dKey.index));

				bars.enter()
					.append("rect")
					.style("fill-opacity", 0);

				bars
					.style("fill", function(d) { return colorMap(keyCurve.get(d), keyAbscissa1(d)); })
					.transition()
					.attr("width", x2.rangeBand())
					.attr("x", function(d) { return x(keyAbscissa0(d)) + x1(keyAbscissa1(d)) + x2(keyAbscissa2(d)); })
					.attr("y", function(d) { return y(d[dKey.value]); })
					.attr("height", function(d) { return h - y(d[dKey.value]); })
					.transition()
					.style("fill-opacity", 1);

				bars.exit().remove();
			} else {
				// data points
				var points = svg.selectAll('circle').data(data, dKey.get(dKey.index));

				points.enter().append('circle')
					.attr("class", function(d) { return "line line-" + keyLegend.get(d); })
					.attr('r', 5);

				points.transition()
					.style("stroke",  function(d) { return colorMap(keyCurve.get(d), keyLegend.get(d)); })
					.attr('cx', function (d) { return x(keyAbscissa(d)); })
					.attr('cy', function (d) { return y(d[dKey.value]); });

				points.exit().remove();

				// data paths
				var curves = svg.selectAll(".curve").data(data_outer, fnKey);

				curves.enter()
					.append("g")
					.attr("class", "curve");
				
				curves.each(curveFn(path));

				curves.exit().remove();

				// confidence intervals (areas)
				var curves_ci = svg.selectAll(".curve_ci").data(showCi ? data_outer : [], fnKey);

				curves_ci.enter()
					.insert("g", ":first-child")
					.attr("class", "curve_ci");
				
				curves_ci.each(curveFn(area_ci));

				curves_ci.exit().remove();

				// legend
				var legend = svg.selectAll(".legend").data(keys_inner, ident);

				legend.enter()
					.append("g")
					.attr("class", function(d) { return "legend legend-" + d; })
					.on("mouseover", mover)
					.on("mouseout", mout)
					.append("text");
				
				legend
					.attr("transform", function(d, i) { return "translate(" + (w + legend_width - 20 * keys_outer.length) + ", " + i * 20 + ")"; })
					.each(legendRow);

				legend.exit().remove();
			}
		});
	}


	function clearChart() {
		d3.select(".chart").selectAll("*").remove();
		svg = null;
	}









	/*
	 * ----- public functions -----
	 */
	
	my.addGraph = function(scope, curveName, tsv) {
		tsvWaiting++;
		scope.push(curveName);
		var scopeId = addScope(scopeTree, scope);
		d3.tsv(tsv, function(error, data) {
			var dateformat = d3.time.format.utc("%Y-%m-%dT%H:%M:%SZ");
			var offset = dataconcat.length;
			data.forEach(function(d, i) {
				d[dKey.param] = +d[dKey.param];
				d[dKey.value] = +d[dKey.value];			
				d[dKey.date] = +dateformat.parse(d[dKey.date]);
				d[dKey.curve] = scopeId;
				d[dKey.index] = offset + i;
			});
			dataconcat = dataconcat.concat(data);
			tsvWaiting--;
			init();
		});
		
		return my;
	}
	
	my.setReady = function() {
		ready = true;
		init();
		return my;
	}
	
	function setFilter(curves) {
		var curveSet = d3.set(curves);
		filter.dim_curve.filterAll();
		filter.dim_curve.filterFunction(function (d) {
			return curveSet.has(d);
		});
		filter.updateAll();
	}

	my.setFilter = function(curve) {
		filter.dim_curve.filter(curve);
		filter.updateAll();
	}

	my.setChartType = function(_chartType, _showCI) {
		chartType = _chartType;
		showCI = isDef(_showCI) ? _showCI : false;
		clearChart();
		filter.updateAll();
		return my;
	}
	
	my.rawdata = function(_) {
		if (!arguments.length) return rawdata;
		rawdata = _;
		return my;
	}

	my.tChart = function() {
		return tChart;
	}
	
	return my;
}())







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