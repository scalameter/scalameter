var ScalaMeter = (function(parent) {
	var my = { name: "main" };

	/*
	 * ----- public functions -----
	 */

	my.init = function() {
    parent.permalink.init(".btn-permalink");
    parent.chart.init(".chart");
		parent.filter.init();

    var filterData = parent.permalink.parseUrl();
		parent.filter.setData(parent.data.index, filterData);
	};

	parent[my.name] = my;

	return parent;
})(ScalaMeter || {});