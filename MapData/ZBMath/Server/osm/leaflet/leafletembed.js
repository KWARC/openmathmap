var map;
var ajaxRequest;
var plotlist;
var plotlayers=[];

function initmap() {
    // set up the map
    map = new L.Map('map');

    // create the tile layer with correct attribution
    var osmUrl='/home/jdoerrie/Desktop/ZBMath/Server/osm/tiles/{z}/{x}/{y}.png';
//    var osmUrl='http://localhost/osm_tiles2/{z}/{x}/{y}.png';
//    var osmUrl='http://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png';
    var osmAttrib='Map data © OpenStreetMap contributors';
    var osm = new L.TileLayer(osmUrl, {minZoom: 10, maxZoom: 13, attribution: osmAttrib});
//    var osm = new L.TileLayer(osmUrl, {minZoom: 3, maxZoom: 18, attribution: osmAttrib});

    // start the map in South-East England
    map.setView(new L.LatLng(0, 0), 10);
//    map.setView(new L.LatLng(51.3, 0.7),9);
    map.addLayer(osm);
}