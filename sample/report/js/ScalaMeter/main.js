var ScalaMeter = (function(parent) {
	var my = { name: "main" };

	/*
	 * ----- public functions -----
	 */

	my.init = function() {
    parent.permalink.init(".btn-permalink");
    var urlData = parent.permalink.parseUrl();
		parent.filter.init(urlData);
	};

	parent[my.name] = my;

	return parent;
})(ScalaMeter || {});