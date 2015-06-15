var cordova = require('cordova'),
    exec = require('cordova/exec');

module.exports = {
    createMapboxTileLayerMap: function createMapboxTileLayerMap(mapId, callback) {
        exec(callback, function (err) {
            callback(err || 'There was a problem.');
        }, "Mapbox", "createMapboxTileLayerMap", [mapId]);
    },
    createMBTilesLayerMap: function createMBTilesLayerMap(file, callback) {
        exec(callback, function (err) {
            callback(err || 'There was a problem.');
        }, "Mapbox", "createMBTilesLayerMap", [file || "test.MBTiles"]);
    },
    mapEditor: function mapEditor(geojson, callback) {
        exec(callback, function (err) {
            callback(err || 'There was a problem.');
        }, "Mapbox", "mapEditor", [geojson]);
    },
    staticMap: function staticMap(geojson, callback) {
        exec(callback, function (err) {
            callback(err || 'There was a problem.');
        }, "Mapbox", "createStaticImage", [geojson]);
    }
};
