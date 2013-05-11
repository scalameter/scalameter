function getUrl() {
	return document.URL + "#" + jQuery.param(cd.allFilters());
}

function getUrlParams() {
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
}