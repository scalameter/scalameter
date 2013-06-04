var ScalaMeter = (function(parent) {
	var my = { name: "main" };

	my.initPermalinkBtn = function(permalinkBtn) {
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

	my.init = function() {
		var allFilters;
		urlParams = my.getUrlParams();
		console.log(urlParams);
		if (urlParams.hasOwnProperty("params")) {
			allFilters = $.parseJSON(urlParams.params);
			console.log(allFilters);
		}
		parent.filter.init(allFilters);
	};

	function getPermalinkUrl() {
		return document.URL.split('#')[0] + "#" + jQuery.param({ params: JSON.stringify(parent.filter.getAllFilters()) });
	}
	my.getUrl = getPermalinkUrl;

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