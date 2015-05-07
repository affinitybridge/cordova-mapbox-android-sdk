package com.affinitybridge.cordova.mapbox;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.mapbox.mapboxsdk.geometry.BoundingBox;
import com.mapbox.mapboxsdk.tileprovider.tilesource.ITileLayer;
import com.mapbox.mapboxsdk.tileprovider.tilesource.MBTilesLayer;
import com.mapbox.mapboxsdk.tileprovider.tilesource.TileLayer;
import com.mapbox.mapboxsdk.tileprovider.tilesource.WebSourceTileLayer;
import com.mapbox.mapboxsdk.views.MapView;

public class OfflineMapActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = this.getIntent();
        String fileName = intent.getStringExtra(Intent.EXTRA_TEXT);

        MapView mapView = new MapView(this);
        BoundingBox box;

        TileLayer mbTileLayer = new MBTilesLayer(this, fileName);
        mapView.setTileSource(new ITileLayer[] {
                mbTileLayer, new WebSourceTileLayer("mapquest",
                "http://otile1.mqcdn.com/tiles/1.0.0/osm/{z}/{x}/{y}.png").setName(
                "MapQuest Open Aerial")
                .setAttribution("Tiles courtesy of MapQuest and OpenStreetMap contributors.")
                .setMinimumZoomLevel(1)
                .setMaximumZoomLevel(18)
        });

        box = mbTileLayer.getBoundingBox();
        mapView.setScrollableAreaLimit(box);
        mapView.setMinZoomLevel(mapView.getTileProvider().getMinimumZoomLevel());
        mapView.setMaxZoomLevel(mapView.getTileProvider().getMaximumZoomLevel());
        mapView.setCenter(mapView.getTileProvider().getCenterCoordinate());
        mapView.setZoom(0);
        Log.d("MapboxPlugin", "zoomToBoundingBox " + box.toString());

        setContentView(mapView);
    }
}
