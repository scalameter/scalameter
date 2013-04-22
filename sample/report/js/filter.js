var curvedata = (function() {
	var my = {},
		dataconcat = [],
		tsvWaiting = 0,
		ready = false,
		scopeTree = { children: [] },
		scopeId = 0,
		rawdata,
		filter_;

	var mainColors = (function() {
		var nGroups = 10;
		var groups = d3.scale.category10().domain(d3.range(nGroups));
		return function(i) {
			return groups(i % nGroups);
		};
	})();

	function init() {
		// init filter once all files have been added to the queue and processed
		if(ready && tsvWaiting == 0) {
			filter_ = createFilter();
			initTree();
		}
	}

	function createFilter() {
		var my = {};

		var cFilter_ = crossfilter(dataconcat),
			filterDim_ = {};

		function getDimension(key) {
			if (!filterDim_.hasOwnProperty(key)) {
				filterDim_[key] = cFilter_.dimension(mapKey(key));
			}
			return filterDim_[key];
		}

		my.getData = function() {
			return getDimension(dKey.curve).top(Infinity);
		};

		my.getDim = getDimension;


		// DATE SELECTION
		var dateDim = getDimension(dKey.date);
		var selectedDates = d3.set();

		function toggleDate(date) {
			console.log(date);
			var wasSelected = selectedDates.has(date);
			d3.select(".badge-" + date).classed("badge-info", !wasSelected);
			if (wasSelected) {
				selectedDates.remove(date);
			} else {
				selectedDates.add(date);
			}
			dateDim.filterFunction(function(d) {
				return selectedDates.has(d);
			});
		}

		var uniqueDates = unique(my.getData(), mapKey(dKey.date), d3.ascending);
		var groupByDay = d3.nest().key(function (d) {
			 return +d3.time.day(new Date(+d));
		}).sortKeys(d3.descending);

		function addBadges(d) {
			var dateBadges = d3.select(this).selectAll(".badge").data(d.values);
			var badgeFormat = d3.time.format("%H:%M:%S");

			dateBadges.enter()
				.append("span")
				.text(function (d) { return badgeFormat(new Date(+d)); })
				.attr("class", function(d) { return "badge badge-" + d; } )
				.on("click", function(d) {
					toggleDate(d);
					update();
				});
		}

		var dayDivs = d3.select(".filters").selectAll("div").data(groupByDay.entries(uniqueDates));
		var divFormat = d3.time.format("%Y-%m-%d");

		dayDivs.enter()
			.append("div");

		dayDivs
			.text(function(d) { return divFormat(new Date(+d.key)); } )
			.each(addBadges);

		toggleDate(uniqueDates[0]);

		return my;
		
		/*
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
			genericChart.update(dim_colors.top(Infinity), ".chart", "value [ms]", mainColors);
			if (isDef(rawdata)) {
				showdata(dim_datetime.top(Infinity));
			}
		}
		
		var chart = d3.selectAll(".filter")
			.data([selectdate, selectparam])
			.each(function(chart) { chart.on("brush", filter.updateAll).on("brushend", filter.updateAll); });
		return filter;
		*/
	}

	function showdata(data) {
		function addCols(row) {
			header.selectAll("th").data(d3.keys(row)).enter().append("th").text(ident);
			d3.select(this).selectAll("td")
				.data(d3.values(row))
				.enter()
				.append("td").text(ident);
		}
		
		var container = d3.select(rawdata);
		
		var header = container.select(".dataheader");
		if (header.empty()) {
			header = container.append("tr").attr("class", "dataheader");
		}
		
		var rows = container.selectAll(".datavalues").data(data, mapKey(dKey.index));
		
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

	function update() {
		var data = filter_.getData();
		genericChart.update(data, ".chart", "value [ms]", mainColors);
		if (isDef(rawdata)) {
			showdata(data);
		}
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
	};
	
	my.setReady = function() {
		ready = true;
		init();
		return my;
	};
	
	function setFilter(curves) {
		var curveSet = d3.set(curves);
		filter_.getDim(dKey.curve).filterFunction(function (d) {
			return curveSet.has(d);
		});
		update();
		return my;
	};

	my.update = function() {
		update();
		return my;
	}

	my.rawdata = function(_) {
		if (!arguments.length) return rawdata;
		rawdata = _;
		return my;
	};

	return my;
}());







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