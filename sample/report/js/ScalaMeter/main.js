var ScalaMeter = (function(parent) {
	var my = { name: "main" };

	my.init = function() {
		parent.filter.createFromIndex(parent.data.index);
	};

	my.getUrl = function() {
		return document.URL + "#" + jQuery.param(cd.allFilters());
	};

	my.getUrlParams = function() {
		var match,
			pl     = /\+/g,
			search = /([^&=]+)=?([^&]*)/g,
			decode = function (s) { return decodeURIComponent(s.replace(pl, " ")); },
			query  = window.location.hash.substring(1);

		var urlParams = {};
		while (match = search.exec(query)) {
			urlParams[decode(match[1])] = decode(match[2]);
		}
		return urlParams;
	};

	parent[my.name] = my;

	return parent;
})(ScalaMeter || {});