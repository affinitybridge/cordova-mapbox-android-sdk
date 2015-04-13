var cordova = require('cordova'),
    exec = require('cordova/exec');

module.exports = {
    echo: function echo(str, callback) {
        exec(callback, function(err) {
            callback('Nothing to echo.');
        }, "Mapbox", "echo", [str]);
    },

    createMap: function createMap(callback) {
        exec(callback, function (err) {
            callback('There was a problem');
        }, "Mapbox", "createMap", []);
    }
};
