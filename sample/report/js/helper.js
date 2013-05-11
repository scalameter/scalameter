var Helper = function() {
	var my = {};

	function toInt(d) {
		return +d;
	}

	function isDef(value) {
		return typeof value !== 'undefined'; 
	}

	my.isDef = isDef;

	my.ident = function(d) {
		return d;
	};

	my.mapKey = function(key) {
		return function(d) { return d[key] };
	};

	my.fKey = function(d) {
		return d.key;
	};

	my.unique = function(data, key, sort){
		var values = d3.nest().key(key).map(data, d3.map).keys();
		values = values.map(toInt);
		if (isDef(sort)) {
			return values.sort(sort);
		} else {
			return values;
		}
	};

	my.dKey = {
		date: "date",
		param: "param-size",
		value: "value",
		success: "success",
		cilo: "cilo",
		cihi: "cihi",
		complete: "complete",
		curve: "curve",
		index: "index"
		/*get: function(key) {
			return function(d) { return d[key] };
		}*/
	};

	return my;

	/*
	var tChart = {
		lineParam: { value: "lineParam", name: "Line Chart (param)" },
		lineDate: { value: "lineDate", name: "Line Chart (date)" },
		bar: { value: "bar", name: "Bar Chart" }
	};
	*/
};