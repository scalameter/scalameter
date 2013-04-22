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
		var uniqueDates = unique(my.getData(), mapKey(dKey.date), d3.descending);
		var selectedDates = d3.set();

		//generate random dates over the past 5 years
		var testUniqueDates = [];
		var today = +new Date();
		for(i=0; i<1000; i++){
		    testUniqueDates.push(today - Math.round(Math.random() * 5 * 365 * 86400 * 1000));
		}
		testUniqueDates.sort(d3.descending);

		// uniqueDates = testUniqueDates;

		function selectedDatesChanged() {
			dateDim.filterFunction(function(d) {
				return selectedDates.has(d);
			});
			my.updateDateFilter();
		}

		function toggleDate(date) {
			if (selectedDates.has(date)) {
				selectedDates.remove(date);
			} else {
				selectedDates.add(date);
			}
			selectedDatesChanged();
		}

		function selectAll() {
			selectedDates = d3.set(uniqueDates);
			selectedDatesChanged();
			update();
		}

		function selectNone() {
			selectedDates = d3.set();
			selectedDatesChanged();
			update();
		}

		function addBadges(d) {
			var dateBadges = d3.select(this).selectAll(".badge").data(d.values, ident);
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
				.classed("badge-info", function(d) { return selectedDates.has(d) });

			dateBadges.exit().remove();
		}

		function days(d) {
			var g = d3.select(this).selectAll("div").data(d.values, fKey);

			g.enter()
				.append("div");
			g
				.text(function(d) { return d3.time.format("%d")(new Date(+d.key)); } )
				.each(addBadges);
			g.exit().remove();
		}

		function months(d) {
			var g = d3.select(this).selectAll("div").data(d.values, fKey);

			g.enter()
				.append("div");
			g
				.text(function(d) { return d3.time.format("%b")(new Date(+d.key)); } )
				.each(days);
			g.exit().remove();
		}

		var keyDay = function (d) { return +d3.time.day(new Date(+d)); };

		var uniqueDays = unique(uniqueDates, keyDay, d3.ascending);
		var nDays = 20; //TODO cst
		var dayFrom = uniqueDays.length - nDays;

		var nestDates = d3.nest()
			.key(function (d) { return +d3.time.year(new Date(+d)); }).sortKeys(d3.descending)
			.key(function (d) { return +d3.time.month(new Date(+d)); }).sortKeys(d3.descending)
			.key(keyDay).sortKeys(d3.descending).sortValues(d3.ascending);

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
			offsetDay = isDef(offsetDay) ? offsetDay : 0;
			dayFrom = dayFrom + offsetDay;
			dayFrom = Math.max(0, Math.min(uniqueDays.length - nDays, dayFrom));

			var nestedDates = nestDates.entries(filterDates());

			console.log(nestedDates);
			var g = d3.select(".filters").selectAll("div").data(nestedDates, fKey);

			g.enter()
				.append("div");
			g
				.text(function(d) { return d3.time.format("%Y")(new Date(+d.key)); } )
				.each(months);
			g.exit().remove();
		}

		var root = d3.select(".filters");

		root.append("button")
			.attr("class", "btn btn-small")
			.text("All")
			.on("click", selectAll);

		root.append("button")
			.attr("class", "btn btn-small")
			.text("None")
			.on("click", selectNone);

		toggleDate(uniqueDates[0]);

		return my;
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

	return my;
}());
