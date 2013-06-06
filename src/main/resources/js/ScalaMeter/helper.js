var ScalaMeter = (function(parent) {
	var my = { name: "helper" };

	var TSV_DATA_KEYS = {
		date: "date",
		paramPrefix: "param-",
		param: "param-size",
		value: "value",
		success: "success",
		cilo: "cilo",
		cihi: "cihi",
		complete: "complete",
		curve: "curve",
		index: "index"
	};

	my.curveKey = mapKey(TSV_DATA_KEYS.curve);

	my.dKey = TSV_DATA_KEYS;

	my.isDef = isDef;

	my.mapKey = mapKey;

	my.mainColors = (function() {
		var nGroups = 10;
		var groups = d3.scale.category10().domain(d3.range(nGroups));
		return function(i) {
			return groups(i % nGroups);
		};
	})();

	my.numberFormat = function(thousandsSeparator) {
		return function(d) {
		    var parts = d.toString().split(".");
		    parts[0] = parts[0].replace(/\B(?=(\d{3})+(?!\d))/g, thousandsSeparator);
		    return parts.join(".");
		};
	}

	my.obj2enum = function(obj) {
		var result = { enumAll: [] };
		for (var key in obj) {
			result[key] = {
				key: key,
				value: obj[key]
			};
			result.enumAll.push(result[key]);
		}
		return result;
	};

	my.ident = function(d) {
		return d;
	};

	my.ascendingToInt = function(a, b) {
		return d3.ascending(+a, +b);
	};

	my.sortBy = function(fn) {
		return function(a, b) {
			return d3.ascending(fn(a), fn(b));
		};
	};

	my.fKey = function(d) {
		return d.key;
	};

	my.fValue = function(d) {
		return d.value;
	};

	my.unique = function(data, key, sort){
		var values = d3.nest().key(key).map(data, d3.map).keys();
		values = values.filter(function(d) { return d != 'null'; }).map(toInt);
		if (isDef(sort)) {
			return values.sort(sort);
		} else {
			return values;
		}
	};

	function isDef(value) {
		return typeof value !== 'undefined'; 
	}

	function mapKey(key) {
		return function(d) { return d.hasOwnProperty(key) ? d[key] : null };
	}

	function toInt(d) {
		return +d;
	}

	parent[my.name] = my;

	return parent;
})(ScalaMeter || {});