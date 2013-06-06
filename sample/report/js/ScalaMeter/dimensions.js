var ScalaMeter = (function(parent) {
	var my = { name: "dimensions" };

	/*
	 * ----- imports -----
	 */
	var h;

	var paramNames_ = [],
		params_ = d3.map();

	my.init = function() {
		h = parent.helper;
	};

	my.addParam = function(name) {
		if (!params_.has(name)) {
			params_.set(name, addDimension(name, name.substr(h.dKey.paramPrefix.length)));
			paramNames_.push(name);
		}
		return my;
	};

	my.filterValues = function(data, legendOrder) {
		paramNames_.forEach(function(name, i) {
			var dim = params_.get(name);
			dim.filteredValues(h.unique(data, dim.keyFn(), i == 0 ? d3.ascending : legendOrder));
		});
	};

	my.addDim = function(name) {
		var newDim = addDimension(name, name);
		params_.set(name, newDim);
		paramNames_.push(name);
		return newDim;
	}

	my.get = function(name) {
		return params_.get(name);
	};

	my.getAll = function() {
		return paramNames_.map(function(name) {
			return params_.get(name);
		});
	};

	my.names = function(_) {
		if (!arguments.length) return paramNames_;
		paramNames_ = _;
		return my;
	};

	function addDimension(name, caption) {
		return (function() {
			var name_ = name,
				caption_ = caption,
				selectMode_ = h.selectModes.single,
				selectedValues_ = d3.set(),
				expanded_ = false,
				format_ = h.numberFormat,
				keyFn_ = h.mapKey(name_),
				filterContainer_,
				values_,
				filteredValues_,
				cfDimension_;

			function updateCrossfilter() {
				cfDimension_.filterFunction(function(d) {
					return d == null || selectedValues_.has(d);
				});
			}

			function updateSelectMode() {
				switch (selectMode_) {
					case h.selectModes.single:
						var selectedValue = values_[0];
						for (var i = 0; i < values_.length; i++) {
							if (selectedValues_.has(values_[i])) {
								selectedValue = values_[i];
								break;
							}
						}
						selectedValues_ = d3.set([selectedValue]);
						break;
					case h.selectModes.all:
						selectedValues_ = d3.set(values_);
						break;
				}
			}

			return {
				init: function(data, cfDimension) {
					values_ = h.unique(data, h.mapKey(name), d3.ascending);

// if (name == h.dKey.date) {
// 	//generate random dates over the past 5 years
// 	var today = +new Date();
// 	for(i=0; i<5000; i++){
// 			values_.push(today - 50 * 86400 * 1000 - Math.round(Math.random() * 5 * 365 * 86400 * 1000));
// 	}
// 	values_.sort(d3.ascending);
// }

					selectedValues_ = d3.set(values_);
					cfDimension_ = cfDimension;
				},

				updateCrossfilter : updateCrossfilter,

				selectMode: function(_) {
					if (!arguments.length) return selectMode_;
					selectMode_ = _;
					updateSelectMode();
				},

				selectedValues: function(_) {
					if (!arguments.length) return selectedValues_;
					selectedValues_ = _;
				},

				filteredValues: function(_) {
					if (!arguments.length) return filteredValues_;
					filteredValues_ = _;
				},

				caption: function(_) {
					if (!arguments.length) return caption_;
					caption_ = _;
				},

				format: function(_) {
					if (!arguments.length) return format_;
					format_ = _;
				},

				filterContainer: function(_) {
					if (!arguments.length) return filterContainer_;
					filterContainer_ = _;
				},

				expanded: function(_) {
					if (!arguments.length) return expanded_;
					expanded_ = _;
				},

				keyFn: function() {
					return keyFn_;
				},

				clickValue: function(value) {
					if (selectMode_ == h.selectModes.single) {
						selectedValues_ = d3.set([value]);
					} else {
						if (selectedValues_.has(value)) {
							selectedValues_.remove(value);
						} else {
							selectedValues_.add(value);
						}
						if (selectedValues_.values().length == values_.length) {
							selectMode_ = h.selectModes.all;
						} else {
							selectMode_ = h.selectModes.select;
						}
					}
				},

				getAllValues: function() {
					return values_;
				},

				selectLast: function() {
					selectedValues_ = d3.set([values_[values_.length - 1]]);
				}
			};
		})();
	}

	parent[my.name] = my;

	return parent;
})(ScalaMeter || {});