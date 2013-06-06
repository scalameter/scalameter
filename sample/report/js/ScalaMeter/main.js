var ScalaMeter = (function(parent) {
	var my = { name: "main" };

	/*
	 * ----- public functions -----
	 */

	my.init = function() {
		parent.permalink.init(getParams);
		parent.chart.init(".chart");
		parent.filter.init();

		var modules = [parent.filter, parent.chart];
		loadModules(modules, parent.filter.update);
	};

	function loadModules(modules, onLoad) {
		var nWaiting = modules.length;

		modules.forEach(function(module) {
			module.load(function() {
				nWaiting--;
				if (nWaiting == 0) {
					onLoad();
				}
			});
		});
	}

	function getParams() {
		return {
			filterConfig: parent.filter.getConfig(),
			chartConfig: parent.chart.getConfig()
		};
	}

	parent[my.name] = my;

	return parent;
})(ScalaMeter || {});