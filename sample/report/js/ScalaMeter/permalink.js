var ScalaMeter = (function(parent) {
	var my = { name: "permalink" };

	/*
	 * ----- public functions -----
	 */

	my.init = function(permalinkBtn) {
		$(permalinkBtn).popover({
			placement: 'bottom',
			trigger: 'manual',
			title: 'Press Ctrl-C to copy',
			html: true
		}).click(function(event) {
			event.preventDefault();
			$(this).data('popover').options.content = '<textarea class="permalink-inner" />';
			$(this).popover('toggle');
			$('.permalink-inner')
				.val(getPermalinkUrl())
				.focus()
				.select()
				.click(function(event) {
					$(this).select();
					event.preventDefault();
				});
		});
		
		$(':not(' + permalinkBtn + ')').click(function(event) {
			if (!event.isDefaultPrevented()) {
				$(permalinkBtn).popover('hide');
			}
		});
	}

	my.parseUrl = function() {
		var allFilters;
		data = getUrlParams();
		if (data.hasOwnProperty("params")) {
			return $.parseJSON(data.params);
		} else {
			return null;
		}
	}

	/*
	 * ----- private functions -----
	 */

	function getPermalinkUrl() {
		var data = {
			params: JSON.stringify(parent.filter.getAllFilters())
		};
		return document.URL.split('#')[0] + "#" + jQuery.param(data);
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
	};

	parent[my.name] = my;

	return parent;
})(ScalaMeter || {});