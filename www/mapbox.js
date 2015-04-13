var cordova = require('cordova'),
    exec = require('cordova/exec');

module.exports = {
    createMap: function createMap(callback) {
        exec(callback, function (err) {
            callback('There was a problem.');
        }, "Mapbox", "createMap", []);
    }
};
