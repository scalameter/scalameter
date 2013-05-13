var CurveData = function(helper, genericChart) {
	var h = helper,
		dKey = h.dKey;

	var my = {},
		dataconcat = [],
		tsvWaiting = 0,
		// ready = false,
		scopeTree = { children: [] },
		scopeId = 0,
		rawdata,
		filter_,
		selectedDates_ = d3.set(),
		selectedCurves_ = d3.set([0]),
		expandedFilter_ = -1;

	var mainColors = (function() {
		var nGroups = 10;
		var groups = d3.scale.category10().domain(d3.range(nGroups));
		return function(i) {
			return groups(i % nGroups);
		};
	})();

	function init() {
		// init filter once all files have been processed
		if(tsvWaiting == 0) {
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
				filterDim_[key] = cFilter_.dimension(h.mapKey(key));
			}
			return filterDim_[key];
		}

		my.getData = function() {
			return getDimension(dKey.curve).top(Infinity);
		};

		my.getDim = getDimension;

		var data = my.getData();

		// DATE SELECTION
		var dateDim = getDimension(dKey.date);
		var uniqueDates = h.unique(data, h.mapKey(dKey.date), d3.descending);



/*
		//generate random dates over the past 5 years
		var testUniqueDates = [];
		var today = +new Date();
		for(i=0; i<1000; i++){
		    testUniqueDates.push(today - Math.round(Math.random() * 5 * 365 * 86400 * 1000));
		}
		testUniqueDates.sort(d3.descending);

		uniqueDates = testUniqueDates;
*/

		function selectedDatesChanged() {
			dateDim.filterFunction(function(d) {
				return selectedDates_.has(d);
			});
			my.updateDateFilter();
		}

		function toggleDate(date) {
			if (selectedDates_.has(date)) {
				selectedDates_.remove(date);
			} else {
				selectedDates_.add(date);
			}
			selectedDatesChanged();
		}

		function selectAll() {
			selectedDates_ = d3.set(uniqueDates);
			selectedDatesChanged();
			update();
		}

		function selectNone() {
			selectedDates_ = d3.set();
			selectedDatesChanged();
			update();
		}

		function addBadges(d) {
			var dateBadges = d3.select(this).selectAll(".badge").data(d.values, h.ident);
			var badgeFormat = d3.time.format("%H:%M:%S");

			dateBadges.enter()
				.append("span")
				.text(function (d) { return badgeFormat(new Date(+d)); })
				.attr("class", function(d) { return "badge badge-" + d; } )
				.on("click", function(d) {
					toggleDate(d);
					update();
				});

			dateBadges
				.classed("badge-info", function(d) { return selectedDates_.has(d) });

			dateBadges.exit().remove();
		}

		function days(d) {
			var g = d3.select(this).selectAll(".day").data(d.values, h.fKey);

			g.enter()
				.append("div")
				.attr("class", "day");
			g
				.text(function(d) { return d3.time.format("%d")(new Date(+d.key)); } )
				.each(addBadges);
			g.exit().remove();
		}

		function months(d) {
			var g = d3.select(this).selectAll(".month").data(d.values, h.fKey);

			g.enter()
				.append("div")
				.attr("class", "month");
			g
				.text(function(d) { return d3.time.format("%b")(new Date(+d.key)); } )
				.each(days);
			g.exit().remove();
		}

		var keyDay = function (d) { return +d3.time.day(new Date(+d)); };

		var uniqueDays = h.unique(uniqueDates, keyDay, d3.ascending);
		var nDays = 20; //TODO cst
		var dayFrom = uniqueDays.length - nDays;

		var nestDates = d3.nest()
			.key(function (d) { return +d3.time.year(new Date(+d)); }).sortKeys(d3.ascending)
			.key(function (d) { return +d3.time.month(new Date(+d)); }).sortKeys(d3.ascending)
			.key(keyDay).sortKeys(d3.ascending).sortValues(d3.ascending);

		function filterDates() {
			var dateFrom = uniqueDays[dayFrom];
			var dayTo = dayFrom + nDays;
			var dateTo = dayTo < uniqueDays.length ? uniqueDays[dayTo] : Infinity;
			console.log(dateFrom);
			console.log(dateTo);
			return uniqueDates.filter(function(d) {
				return d >= dateFrom && d < dateTo;
			});
		}

		my.updateDateFilter = function(offsetDay) {
			offsetDay = h.isDef(offsetDay) ? offsetDay : 0;
			dayFrom = dayFrom + offsetDay;
			dayFrom = Math.max(0, Math.min(uniqueDays.length - nDays, dayFrom));

			var nestedDates = nestDates.entries(filterDates());
			nestedDates.map(function(year) {
				year.nDays = 0;
				year.values.map(function(month) {
					month.nDays = month.values.length;
					year.nDays = year.nDays + month.nDays;
					return month;
				});
				return year;
			});
			console.log(nestedDates);

			var g = d3.select(".filters").selectAll(".year").data(nestedDates, h.fKey);

			g.enter()
				.append("div")
				.attr("class", "year");
			g
				.text(function(d) { return d3.time.format("%Y")(new Date(+d.key)); } )
				.each(months);
			g.exit().remove();
		}

		var root = d3.select(".filters");

/*
		root.append("button")
			.attr("class", "btn btn-small")
			.text("All")
			.on("click", selectAll);

		root.append("button")
			.attr("class", "btn btn-small")
			.text("None")
			.on("click", selectNone);

		toggleDate(uniqueDates[0]);
		*/

		function addFilter(root, name, fKey, i) {
			var content = '<div class="filter-container-header">' +
				'<i class="icon-move"></i> ' + name +
			'</div>' +
			'<div class="tabbable tabs-below">' +
				'<div class="tab-content">' +
				  '<i class="filter-expand icon-chevron-down filter-showexpanded"></i>' +
				  '<i class="filter-expand icon-chevron-right filter-showcollapsed"></i>' +
					'<span class="values"></span>' +
				'</div>' +
				'<ul class="nav nav-tabs filter-showexpanded">' +
					'<li class="active"><a data-toggle="tab">All</a></li>' +
					'<li><a data-toggle="tab">Single</a></li>' +
					'<li><a data-toggle="tab">Select</a></li>' +
				'</ul>' +
			'</div>';

			var container = root.append("div");

			container
				.datum(i)
				.attr("class", "filter-container filter-collapsed")
				.html(content);

			var values = h.unique(data, fKey, d3.ascending);
			values = values.concat(values.map(function(d) { return d / 1000; })).concat(values);

			var badges = container.select(".values").selectAll(".badge").data(values);

			badges.enter()
				.append("span")
				.attr("class", "badge")
				.text(h.ident);

			container.selectAll(".filter-expand")
				.on("click", function() {
					if (expandedFilter_ == i) {
						expandedFilter_ = -1;
					} else {
						expandedFilter_ = i;
					}
					d3.selectAll(".filter-container")
						.classed("filter-expanded", function(d) { return d == expandedFilter_; })
						.classed("filter-collapsed", function(d) { return d != expandedFilter_; });
				});

			console.log(values);
		}

		addFilter(root, "size0", h.mapKey(dKey.param), 0);
		addFilter(root, "size1", h.mapKey(dKey.param), 1);




		return my;
	}

	function showdata(data) {
		function addCols(row) {
			header.selectAll("th").data(d3.keys(row)).enter().append("th").text(h.ident);
			d3.select(this).selectAll("td")
				.data(d3.values(row))
				.enter()
				.append("td").text(h.ident);
		}
		
		var container = d3.select(rawdata);
		
		var header = container.select(".dataheader");
		if (header.empty()) {
			header = container.append("tr").attr("class", "dataheader");
		}
		
		var rows = container.selectAll(".datavalues").data(data, h.mapKey(dKey.index));
		
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
		selectedCurves_.forEach(function(d) {
			$(".tree").dynatree("getTree").selectKey(d);
		});		
	}
	
	function addScope(node, scope) {
		var nodeName = scope[0];
		var isLeaf = scope.length == 1;
		var id = isLeaf ? scopeId : -1;
		if (!node.children.hasOwnProperty(nodeName)) {
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
		if (h.isDef(rawdata)) {
			showdata(data);
		}
	}


	/*
	 * ----- public functions -----
	 */
	
	/*
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
	*/

	my.setCurves = function(curves) {
		tsvWaiting = curves.length;
		curves.forEach(function(curve) {
			curve.scope.push(curve.name);
			var scopeId = addScope(scopeTree, curve.scope);
			d3.tsv(curve.file, function(error, data) {
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
		});
		return my;
	};
	
	/*
	my.setReady = function() {
		ready = true;
		init();
		return my;
	};
	*/
	
	function setFilter(curves) {
		selectedCurves_ = d3.set(curves);
		filter_.getDim(dKey.curve).filterFunction(function (d) {
			return selectedCurves_.has(d);
		});
		update();
		return my;
	};

	my.update = function() {
		update();
		return my;
	}

	my.prevDay = function() {
		filter_.updateDateFilter(1);
		return my;
	}

	my.nextDay = function() {
		filter_.updateDateFilter(-1);
		return my;
	}

	my.rawdata = function(_) {
		if (!arguments.length) return rawdata;
		rawdata = _;
		return my;
	};

	my.allFilters = function(_) {
		if (!arguments.length) {
			// get
			return {
				curves: selectedCurves_.values(),
				dates: selectedDates_.values()
			};
		} else {
			// set
			//TODO
		}
		return my;
	}

	return my;
};
