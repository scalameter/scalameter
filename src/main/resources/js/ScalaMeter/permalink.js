var ScalaMeter = (function(parent) {
	var my = { name: "permalink" };

	var BITLY_ACCESS_TOKEN = "34fda5dc3ef2ea36e6caf295f4a6443b4afa7401";

	var getParams_,
		storedData_;

	/*
	 * ----- public functions -----
	 */

	my.init = function(getParams) {
		getParams_ = getParams;
		storedData_ = parseUrl();

		var permalinkBtn = ".btn-permalink";

		$(permalinkBtn).popover({
			placement: 'bottom',
			trigger: 'manual',
			title: 'Press Ctrl-C to copy <a class="permalink-shorten pull-right">Shorten</a>',
			html: true
		}).click(function(event) {
			event.preventDefault();
			$(this).data('popover').options.content = '<textarea class="permalink-inner" />';
			$(this).popover('toggle');
			longUrl = getPermalinkUrl();
			displayUrl(longUrl);
			if (isOnLocalhost()) {
				$('.permalink-shorten').hide();
			} else {
				$('.permalink-shorten').click(function(event) {
					event.preventDefault();
					bitlyShorten(longUrl, function(shortUrl) {
						displayUrl(shortUrl);
					});
				});
			}
		});
		
		$(':not(' + permalinkBtn + ')').click(function(event) {
			if (!event.isDefaultPrevented()) {
				$(permalinkBtn).popover('hide');
			}
		});
	};

	my.storedData = function() {
		return storedData_;
	};

	/*
	 * ----- private functions -----
	 */

	function displayUrl(url) {
		$('.permalink-inner')
			.val(url)
			.focus()
			.select()
			.click(function(event) {
				$(this).select();
				event.preventDefault();
			});
	}

	function getPermalinkUrl() {
		var data = {
			params: JSON.stringify(getParams_())
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

	function bitlyShorten(longUrl, func) {
		$.getJSON(
			"https://api-ssl.bitly.com/v3/shorten", 
			{ 
				"access_token": BITLY_ACCESS_TOKEN,
				"longUrl": longUrl
			},
			function(response) {
				func(response.data.url);
			}
		);
	}

	function isOnLocalhost() {
		var hostname = document.location.hostname;
		return hostname == "127.0.0.1" || hostname == "localhost";
	}

	function parseUrl() {
		var data = getUrlParams();
		if (data.hasOwnProperty("params")) {
			return $.parseJSON(data.params);
		} else {
			return null;
		}
	}

	parent[my.name] = my;

	return parent;
})(ScalaMeter || {});