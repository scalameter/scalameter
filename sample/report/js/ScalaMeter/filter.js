var ScalaMeter = (function(parent) {
	var my = { name: "filter" };

	/*
	 * ----- imports -----
	 */
	var h,
		dKey;

	/*
	 * ----- constants -----
	 */
	var SELECT_MODES,
		TSV_DATE_FORMAT,
		NUMBER_FORMAT,
		DATE_FORMAT,
		DATE_FILTER_WIDTH;

	/*
	 * ----- private fields -----
	 */
	var	dataConcat_,
		// rawdata,
		filter_,
		dateDim_,
		selectedCurves_;

	/*
	 * ----- public functions -----
	 */
	
	my.init = function() {
		h = parent.helper;
		dKey = h.dKey;

		SELECT_MODES = h.obj2enum({
			single: "Single",
			select: "Select",
			all: "All"
		});
		TSV_DATE_FORMAT = d3.time.format.utc("%Y-%m-%dT%H:%M:%SZ");
		NUMBER_FORMAT = h.numberFormat("\u202F");  // 202F: narrow no-break space
		DATE_FORMAT = (function(d) {return d3.time.format("%Y-%m-%d %H:%M:%S")(new Date(+d)); });
		DATE_FILTER_WIDTH = 12;

		dataConcat_ = [];
		selectedCurves_ = d3.set([0]);
	};

	my.setData = function(index, filterData) {
		createFromIndex(index, filterData);
	};

	my.update = function() {
		updateChart();
	}

/*
	my.rawdata = function(_) {
		if (!arguments.length) return rawdata;
		rawdata = _;
		return my;
	};
	*/

	my.getFilterData = function() {
		return {
			curves: selectedCurves_.values(),
			order: filterDimensions.names(),
			filters: filterDimensions.getAll().map(function(dim) {
				return dim.selectedValues().values();
			})
		};
		return my;
	};

	/*
	 * ----- private functions -----
	 */
	
	var filterDimensions = (function() {
		var my = {};

		var paramNames_ = [],
			params_ = d3.map();

		function addDimension(name, caption) {
			return (function() {
				var name_ = name,
					caption_ = caption,
					selectMode_ = SELECT_MODES.single,
					selectedValues_ = d3.set(),
					expanded_ = false,
					format_ = NUMBER_FORMAT,
					filterContainer_,
					values_,
					filteredValues_,
					cfDimension_;

				function updateCrossfilter() {
					cfDimension_.filterFunction(function(d) {
						return d == null || selectedValues_.has(d);
					});
				}

				function updateSelectMode() {
					switch (selectMode_) {
						case SELECT_MODES.single:
							var selectedValue = values_[0];
							for (var i = 0; i < values_.length; i++) {
								if (selectedValues_.has(values_[i])) {
									selectedValue = values_[i];
									break;
								}
							}
							selectedValues_ = d3.set([selectedValue]);
							break;
						case SELECT_MODES.all:
							selectedValues_ = d3.set(values_);
							break;
					}
				}

				return {
					init: function(data, cfDimension) {
						values_ = h.unique(data, h.mapKey(name), d3.ascending);

// if (name == dKey.date) {
// 	//generate random dates over the past 5 years
// 	values_ = [];
// 	var today = +new Date();
// 	for(i=0; i<100; i++){
// 			values_.push(today - Math.round(Math.random() * 40 * 365 * 86400 * 1000));
// 	}
// 	values_.sort(d3.ascending);
// }

						selectedValues_ = d3.set(values_);
						cfDimension_ = cfDimension;
					},

					updateCrossfilter : updateCrossfilter,

					selectMode: function(_) {
						if (!arguments.length) return selectMode_;
						selectMode_ = _;
						updateSelectMode();
					},

					selectedValues: function(_) {
						if (!arguments.length) return selectedValues_;
						selectedValues_ = _;
					},

					filteredValues: function(_) {
						if (!arguments.length) return filteredValues_;
						filteredValues_ = _;
					},

					caption: function(_) {
						if (!arguments.length) return caption_;
						caption_ = _;
					},

					format: function(_) {
						if (!arguments.length) return format_;
						format_ = _;
					},

					filterContainer: function(_) {
						if (!arguments.length) return filterContainer_;
						filterContainer_ = _;
					},

					expanded: function(_) {
						if (!arguments.length) return expanded_;
						expanded_ = _;
					},

					keyFn: function() {
						return h.mapKey(name_);
					},

					clickValue: function(value) {
						if (selectMode_ == SELECT_MODES.single) {
							selectedValues_ = d3.set([value]);
						} else {
							if (selectedValues_.has(value)) {
								selectedValues_.remove(value);
							} else {
								selectedValues_.add(value);
							}
							if (selectedValues_.values().length == values_.length) {
								selectMode_ = SELECT_MODES.all;
							} else {
								selectMode_ = SELECT_MODES.select;
							}
						}
					},

					getAllValues: function() {
						return values_;
					},

					selectLast: function() {
						selectedValues_ = d3.set([values_[values_.length - 1]]);
					}
				};
			})();
		}

		my.addParam = function(name) {
			if (!params_.has(name)) {
				params_.set(name, addDimension(name, name.substr(dKey.paramPrefix.length)));
				paramNames_.push(name);
			}
			return my;
		};

		my.filterValues = function(data, legendOrder) {
			paramNames_.forEach(function(name, i) {
				var dim = params_.get(name);
				dim.filteredValues(h.unique(data, dim.keyFn(), i == 0 ? d3.ascending : legendOrder));
			});
		};

		my.addDim = function(name) {
			var newDim = addDimension(name, name);
			params_.set(name, newDim);
			paramNames_.push(name);
			return newDim;
		}

		my.get = function(name) {
			return params_.get(name);
		};

		my.getAll = function() {
			return paramNames_.map(function(name) {
				return params_.get(name);
			});
		};

		// my.getAxisParam = function() {
		// 	return params_.get(paramNames_[0]);
		// };

		my.names = function(_) {
			if (!arguments.length) return paramNames_;
			paramNames_ = _;
			return my;
		};

		return my;
	})();

	function initFilters() {
		var my = {};

		var cFilter_ = crossfilter(dataConcat_),
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

		my.updateFilters = updateFilters;

		function timesOfDay(d) {
			var dateBadges = d3.select(this).selectAll(".timeofday").data(d.values, h.ident);
			var badgeFormat = d3.time.format("%H:%M:%S");

			dateBadges.enter()
				.append("span")
				.text(function (d) { return badgeFormat(new Date(+d)); })
				.attr("class", "label timeofday filter-value")
				.each(updateLabels(dateDim_))
				.on("click", function(d) {
					clickBadge(dateDim_, d);
				});

			dateBadges.exit().remove();
		}

		function days(d) {
			var g = d3.select(this).selectAll(".day").data(d.values, h.fKey);

			g.enter()
				.append("div")
				.attr("class", "day");
			g
				.text(function(d) { return d3.time.format("%d")(new Date(+d.key)); } )
				.order()
				.each(timesOfDay);
			g.exit().remove();
		}

		function months(d) {
			var g = d3.select(this).selectAll(".month").data(d.values, h.fKey);

			g.enter()
				.append("div")
				.attr("class", "month")
				.append("div")
				.attr("class", "caption-outer")
				.append("span")
				.attr("class", "caption");

			g.selectAll(".caption")
				.text(function(d) { return d3.time.format("%b")(new Date(+d.key)); } )

			g
				.order()
				.each(days);
			g.exit().remove();
		}

		function filterDates() {
			var dateFrom = uniqueDays[dayFrom];
			var dayTo = dayFrom + DATE_FILTER_WIDTH;
			var dateTo = dayTo < uniqueDays.length ? uniqueDays[dayTo] : Infinity;
			return dateDim_.getAllValues().filter(function(d) {
				return d >= dateFrom && d < dateTo;
			});
		}

		function updateDateFilter(container) {
			dayFrom = Math.max(0, Math.min(uniqueDays.length - DATE_FILTER_WIDTH, dayFrom));
			var dateList = dateDim_.expanded() ? filterDates() : dateDim_.selectedValues().values();

			var nestedDates = nestDates.entries(dateList);

			var g = container.select(".filter-values").selectAll(".year").data(nestedDates, h.fKey);

			g.enter()
				.append("div")
				.attr("class", "year")
				.append("div")
				.attr("class", "caption-outer")
				.append("span")
				.attr("class", "caption");

			g.selectAll(".caption")
				.text(function(d) { return d3.time.format("%Y")(new Date(+d.key)); } );	

			g
				.order()
				.each(months);
			g.exit().remove();
		}

		function updateSelection(param) {
			param.updateCrossfilter();
			updateFilters();
			updateChart();
		}

		function clickBadge(dim, d) {
			if (!dim.expanded()) {
				toggleExpanded(dim);
			} else {
				dim.clickValue(d);
				updateSelection(dim);
			}
		}

		function toggleExpanded(dim) {
			dim.expanded(!dim.expanded());
			var container = dim.filterContainer();
			container
				.classed("filter-expanded", dim.expanded())
				.classed("filter-collapsed", !dim.expanded());
			if (dim == dateDim_) {
				updateDateFilter(container);
			}
		}

		function createFilter(paramName, i) {
			var container = d3.select(this);

			var param = filterDimensions.get(paramName);
			param.filterContainer(container);
			if (param == dateDim_) {
				param.selectLast();
				param.selectMode(SELECT_MODES.single);
			} else {
				param.init(data, getDimension(paramName));
				param.selectMode(i == 0 ? SELECT_MODES.select : SELECT_MODES.single);
			}
			param.updateCrossfilter();

			var content = '' +
				'<div class="filter-container-header">' +
					param.caption() +
				'</div>' +
				'<div class="tabbable tabs-below">' +
					'<div class="tab-content">' +
						'<i class="filter-expand icon-chevron-down filter-hidecollapsed"></i>' +
						'<i class="filter-expand icon-chevron-right filter-hideexpanded"></i>' +
						'<span class="filter-values"></span>' +
					'</div>' +
					'<ul class="nav nav-tabs filter-hidecollapsed"></ul>' +
				'</div>';

			container
				.attr("id", h.ident)
				.attr("class", "filter-container filter-collapsed")
				.html(content);

			container.select("ul").selectAll("li").data(SELECT_MODES.enumAll)
				.enter()
				.append("li")
				.append("a")
				.attr("data-toggle", "tab")
				.text(h.fValue)
				.on("click", function(d) {
					param.selectMode(d);
					updateSelection(param);
				});

			container.selectAll(".filter-expand")
				.on("click", function() {
					toggleExpanded(param);
				});

			var valuesRoot = container.select(".filter-values");
			if (param == dateDim_) {
				valuesRoot
					.append("div")
					.attr("class", "date-slider");
			} else {
				var badges = valuesRoot.selectAll(".filter-value").data(param.getAllValues());

				badges.enter()
					.append("span")
					.attr("class", "label filter-value")
					.on("click", function(d) {
						clickBadge(param, d);
					})
					.text(NUMBER_FORMAT);
			}
		}

		function updateLabels(dim) {
			return function(d) {
				var selected = dim.selectedValues().has(d);
				d3.select(this)
					.classed("label-info", selected)
					.classed("filter-hidecollapsed", !selected);
			};
		}

		function updateFilter(paramName) {
			var dim = filterDimensions.get(paramName);
			var container = d3.select(this);

			if (dim == dateDim_) {
				updateDateFilter(container);
			}

			container.select("ul").selectAll("li")
				.classed("active", function(d) { return d == dim.selectMode(); });

			container.selectAll(".filter-value")
				.each(updateLabels(dim));
		}

		function updateFilters() {
			var containers = root.selectAll(".filter-container").data(filterDimensions.names(), h.ident);

			containers
				.order()
				.each(updateFilter);
		}

		function createFilters() {
			var containers = root.selectAll(".filter-container").data(filterDimensions.names(), h.ident);

			containers.enter()
				.append("div")
				.each(createFilter);			
		}

		var root = d3.select(".filters");

		my.getDim = getDimension;

		var data = my.getData();

		dateDim_ = filterDimensions.addDim(dKey.date);
		dateDim_.init(data, getDimension(dKey.date));
		dateDim_.format(DATE_FORMAT);

		var keyDay = function (d) { return +d3.time.day(new Date(+d)); };

		var uniqueDays = h.unique(dateDim_.getAllValues(), keyDay, d3.ascending);
		var dayFrom = uniqueDays.length - DATE_FILTER_WIDTH;

		var nestDates = d3.nest()
			.key(function (d) { return +d3.time.year(new Date(+d)); }).sortKeys(h.ascendingToInt)
			.key(function (d) { return +d3.time.month(new Date(+d)); }).sortKeys(h.ascendingToInt)
			.key(keyDay).sortKeys(h.ascendingToInt).sortValues(d3.ascending);

		createFilters();

		$(".filters").sortable({
			handle: ".filter-container-header",
			update: function(event, ui) {
				filterDimensions.names($(this).sortable("toArray"));
				updateChart();
			}
		});
		$(".filters").disableSelection();

		$(".date-slider").slider({
			orientation: "horizontal",
			min: 0,
			max: dayFrom,
			value: dayFrom,
			slide: function (event, ui) {
				dayFrom = ui.value;
				updateDateFilter(dateDim_.filterContainer());
			}
		});

		filter_ = my;
	}

/*
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
	*/

	/*
	 * Initialize the dynatree widget, using the
	 * data accumulated in scopeTree.
	 */
	function initTree(scopeTree) {
		var children = convertTree(scopeTree, "");

		$(".tree").dynatree({
			onSelect: function(flag, node) {
				var selectedNodes = node.tree.getSelectedNodes();
				var selectedKeys = $.map(selectedNodes, function(node){
					return +node.data.key;
				});
				selectedCurves_ = d3.set(selectedKeys);
				updateCurveDim();
				updateChart();
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

		function convertTree(node, title) {
			var children = [];
			for (var child in node.children) {
				children.push(
					convertTree(node.children[child], child)
				);
			}
			if (title != "") {
				title = '<span class="dynatree-adjtext">' + title + '</span>';
				if (node.id != -1) {
					var color = h.mainColors(node.id);
					title = '<div class="dynatree-square" style="background-color:' + color + '"></div>' + title;
				}
				return {
					key: "" + node.id,
					title: title,
					expand: true,
					select: selectedCurves_.has(node.id),
					children: children
				}
			} else {
				return children;
			}
		}
	}

	function updateChart() {
		parent.chart.update(filter_.getData(), filterDimensions, dateDim_);
		/*if (h.isDef(rawdata)) {
			showdata(data);
		}*/
	}

	function createFromIndex(index, allFilters) {
		var tsvWaiting = index.length;
		var scopeTree = { children: [] };

		index.forEach(function(curve, id) {
			curve.scope.push(curve.name);
			addScope(scopeTree, curve.scope, id);
			d3.tsv(curve.file, function(error, data) {
				if (data.length != 0) {
					parseData(data, id);
				}
				tsvReady();
			});
		});

		function tsvReady() {
			tsvWaiting--;
			// init filter once all files have been processed
			if (tsvWaiting == 0) {
				initFilters();
				if (allFilters != null) {
					setAllFilters(allFilters);
				}
				filter_.updateFilters();
				initTree(scopeTree);
				updateCurveDim();
				updateChart();
			}
		}

		function addScope(node, scope, leafId) {
			var nodeName = scope[0];
			var isLeaf = scope.length == 1;
			if (!node.children.hasOwnProperty(nodeName)) {
				node.children[nodeName] = {
					"id": isLeaf ? leafId : -1,
					"children": []
				}
			}
			if (!isLeaf) {
				scope.shift();
				addScope(node.children[nodeName], scope, leafId);
			}
		}

		function parseData(data, curveId) {
			for (var key in data[0]) {
				if (key.slice(0, dKey.paramPrefix.length) == dKey.paramPrefix) {
					// key starts with "param-"
					filterDimensions.addParam(key);
				}
			}
			var offset = dataConcat_.length;
			data.forEach(function(d, i) {
				filterDimensions.names().forEach(function(paramName) {
					if (d.hasOwnProperty(paramName)) {
						d[paramName] = +d[paramName];
					}
				});
				d[dKey.value] = +d[dKey.value];			
				d[dKey.date] = +TSV_DATE_FORMAT.parse(d[dKey.date]);
				d[dKey.curve] = curveId;
				d[dKey.index] = offset + i;
			});
			dataConcat_ = dataConcat_.concat(data);
		}
	}

	function setAllFilters(_) {
		selectedCurves_ = d3.set(_.curves);
		filterDimensions.names(_.order);
		for (var i = 0; i < _.order.length; i++) {
			var dim = filterDimensions.get(_.order[i]);
			dim.selectMode(SELECT_MODES.select);
			dim.selectedValues(d3.set(_.filters[i]));
			dim.updateCrossfilter();
		};
	}
	
	function updateCurveDim() {
		filter_.getDim(dKey.curve).filterFunction(function (d) {
			return selectedCurves_.has(d);
		});
	}

	parent[my.name] = my;

	return parent;
})(ScalaMeter || {});