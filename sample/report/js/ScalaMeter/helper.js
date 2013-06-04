var ScalaMeter = (function(parent) {
	var my = { name: "helper" };

	function toInt(d) {
		return +d;
	}

	function isDef(value) {
		return typeof value !== 'undefined'; 
	}

	my.isDef = isDef;

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
		for (key in obj) {
			result[key] = {
				key: key,
				value: obj[key]
			};
			result.enumAll.push(result[key]);
		}
		console.log(result);
		return result;
	};

	my.ident = function(d) {
		return d;
	};

	// my.arrayToString = function(array) {
	// 	return "[" + array.map(function(d) { return '"' + d + '"'; }).join(",") + "]";
	// };

	my.ascendingToInt = function(a, b) {
		return d3.ascending(+a, +b);
	};

	my.sortBy = function(fn) {
		return function(a, b) {
			return d3.ascending(fn(a), fn(b));
		};
	};

	my.mapKey = function(key) {
		return function(d) { return d.hasOwnProperty(key) ? d[key] : null };
	};

	my.fKey = function(d) {
		return d.key;
	};

	my.fValue = function(d) {
		return d.value;
	};

	my.unique = function(data, key, sort){
		var values = d3.nest().key(key).map(data, d3.map).keys();
		// console.log(values);
		values = values.filter(function(d) { return d != 'null'; }).map(toInt);
		if (isDef(sort)) {
			return values.sort(sort);
		} else {
			return values;
		}
	};

	my.dKey = {
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

	/*
	var tChart = {
		lineParam: { value: "lineParam", name: "Line Chart (param)" },
		lineDate: { value: "lineDate", name: "Line Chart (date)" },
		bar: { value: "bar", name: "Bar Chart" }
	};
	*/
	parent[my.name] = my;

	return parent;
})(ScalaMeter || {});