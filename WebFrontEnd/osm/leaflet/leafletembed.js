/**
 * Copyright (c) 2013-19 KWARC Group <kwarc.info>
 *
 * This file is part of OpenMathMap.
 *
 * OpenMathMap is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * OpenMathMap is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenMathMap. If not, see <http://www.gnu.org/licenses/>.
 */

var mathservice = '/MathService/'

var map;
var ajaxRequest;
var plotlist;
var plotlayers=[];

function initmap() {
    // set up the map
    map = new L.Map('map');

    // create the tile layer with correct attribution
    var osmUrl='/osm/Political/Tiles/{z}/{x}/{y}.png';
    var osmAttrib='Map data (c) OpenStreetMap contributors';
    var osm = new L.TileLayer(osmUrl, {minZoom: 10, maxZoom: 14, attribution: osmAttrib});

    // start the map in the center with zoom level 10
    map.setView(new L.LatLng(0.5, 0.5), 10);
    map.addLayer(osm);
    var x, y;
    map.on('click', function (e) {
        x = e.latlng.lat;
        y = e.latlng.lng;
	z = map.getZoom();
        latlng = e.latlng;
        $.ajax({
   	    url: mathservice + "mscquery/?lat=" + x + "&long=" + y + "&zoom=" + z,
	    headers: {
		jsonp: 'application/javascript'
	    },
            accepts: 'application/javascript',
            dataType: 'jsonp',
            contentType: 'application/javascript',
            crossDomain: true,
            type: 'GET',
	    jsonpCallback: 'getMsc'
	});
    });

}

var getMsc = function (data) {
    var popup = L.popup();
	mscJson = data;
	if (data.name != 'null') {
		var name = mscJson.name.substring(3);
		var planetmath = "http://planetmath.org/msc_browser/"+ name;
		var zentralblatt = "http://www.zentralblatt-math.org/msc/en/search/?pa=" + name.replace("-XX","");
		console.log(zentralblatt);
		popup
		    .setLatLng(latlng)
		    .setContent(mscJson.name + ': ' + mscJson.description + '<br>Goto: <br> <a href="'+ planetmath + '">  PlanetMath  </a><br> <a href="' + zentralblatt + '">Zentralblatt</a>')
		    .openOn(map);
	}
};



$(document).ready(main);

function main () {
	$('.browser').bind('click', function () {
		$('.Major').toggle();
		$('.Second').hide();
	});
	searchListen();
	buildMenu();
}

function buildMenu() {
	var bla;
	console.log("buildmenue");
	$.ajax({
		type: 'GET',
		url: 'msc-medium.xml',
		dataType: 'xml',
		beforeSend: function (xhr) {
			//console.log("before");
		}
	}).done(function (data) {
		bla = data;
		console.log(data);
		var text = '';
		$(data).find("Major").each(function() {
			var name = $(this).find("name").first().contents().text(),
				number = $(this).find("number").first().contents().text();
			text += '<tr id = "'+ number +'" class="Major"><td>' + name + '</td><td>' + number + '</td><tr>';

			$(this).find("Second").each(function() {
				var sname = $(this).find("name").first().contents().text(),
				snumber = $(this).find("number").first().contents().text();
				text += '<tr id = '+ snumber +' class="Second '+ number +'"><td>.....'+ sname +'</td><td>'+ snumber +'</td></tr>';

			});
		});

		$('.classes').append(text);

		$('.Major').bind('click', function () {
			console.log($('.' + $(this).attr('id')));
			$('.' + $(this).attr('id')).toggle();
			console.log($(this).attr('id'));
		});
	});
}

function searchListen() {
	$('#mscSearch').bind("enterKey",function(e) {
		getPosition($.trim($('#mscSearch').val()));
	});
	$('#mscSearch').keyup(function(e){
		if(e.keyCode == 13) {
		  $(this).trigger("enterKey");
		}
	});
}

function getPosition(e) {
	if (e.length > 1) {
		marker = L.marker([0.5, 0.5]);
		marker.addTo(map);
	} else {
		map.removeLayer(marker);
	}
}

