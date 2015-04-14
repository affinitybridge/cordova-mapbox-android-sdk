# cordova-mapbox-android-sdk
Cordova plugin for Mapbox SDK (Currently android only)

### Usage:
    // Wait until Cordova & plugins are loaded.
    window.document.addEventListener('deviceready', function() {
        // Create Mapbox map.
        mapbox.createMapboxTileLayerMap('mapbox.streets', function () {
            alert("Map created");
        });

        // or...

        // Create MBTiles offline map.
        mapbox.createMBTilesMap('my-tiles.MBTiles', function () {
            alert("Map created");
        });
    }, false);

### Installing:
    // Set Mapbox access token.
    export MAPBOX_ACCESS_TOKEN=<your-mapbox-access-token>
    export MAPBOX_PLUGIN_PATH=<path-to-plugin>
    // Build with experimental Gradle support.
    export ANDROID_BUILD=gradle cordova build android
    // Requires Cordova >=4.3.0 and Cordova Android 4.0.x.
    cordova platform add android@4.0.x --usegit
    cordova plugin add $MAPBOX_PLUGIN_PATH --variable MAPBOX_ACCESS_TOKEN=$MAPBOX_ACCESS_TOKEN
    // Build and deploy.
    cordova run android

### Developing:
    // Remove old version of plugin.
    cordova plugin remove com.affinitybridge.cordova.mapbox
    // Install latest version of plugin.
    cordova plugin add $MAPBOX_PLUGIN_PATH --variable MAPBOX_ACCESS_TOKEN=$MAPBOX_ACCESS_TOKEN
    // Build and deploy.
    cordova run android
