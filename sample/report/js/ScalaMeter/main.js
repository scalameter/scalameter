var ScalaMeter = (function(parent) {
	var my = { name: "main" };

	/*
	 * ----- public functions -----
	 */

	my.init = function() {
		parent.permalink.init(getParams);
		parent.chart.init(".chart");
		parent.filter.init();

		parent.filter.load(updateView);
		parent.chart.load();
	};

	function getParams() {
		return {
			filterConfig: parent.filter.getConfig(),
			chartConfig: parent.chart.getConfig()
		};
	}

	function updateView() {
		parent.filter.update();
	}

	parent[my.name] = my;

	return parent;
})(ScalaMeter || {});