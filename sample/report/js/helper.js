function isDef(value) {
	return typeof value !== 'undefined'; 
}

function ident(d) {
	return d;
}

function unique(data, key, sort){
	var values = d3.nest().key(key).map(data, d3.map).keys();
	if (isDef(sort)) {
		return values.sort(sort);
	} else {
		return values;
	}
}

var dKey = {
	date: "date",
	param: "param-size",
	value: "value",
	success: "success",
	cilo: "cilo",
	cihi: "cihi",
	complete: "complete",
	curve: "curve",
	index: "index",
	get: function(key) {
		return function(d) { return d[key] };
	}
};

/*
var tChart = {
	lineParam: { value: "lineParam", name: "Line Chart (param)" },
	lineDate: { value: "lineDate", name: "Line Chart (date)" },
	bar: { value: "bar", name: "Bar Chart" }
};
*/
