package com.affinitybridge.cordova.mapbox;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.view.MenuInflater;
import android.view.Menu;

import android.util.Log;

import com.mapbox.mapboxsdk.geometry.BoundingBox;
import com.mapbox.mapboxsdk.views.MapView;
import com.mapbox.mapboxsdk.tileprovider.tilesource.ITileLayer;
import com.mapbox.mapboxsdk.tileprovider.tilesource.TileLayer;
import com.mapbox.mapboxsdk.tileprovider.tilesource.MapboxTileLayer;
import com.mapbox.mapboxsdk.tileprovider.tilesource.MBTilesLayer;
import com.mapbox.mapboxsdk.tileprovider.tilesource.WebSourceTileLayer;

public class MapEditorActivity extends Activity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Resources res = this.getResources();
    int resId = res.getIdentifier("mapboxAccessToken", "string", this.getPackageName());
    String accessToken = res.getString(resId);

    MapView mapView = new MapView(this);
    BoundingBox box;

    mapView.setAccessToken(accessToken);
    TileLayer mbTileLayer = new MapboxTileLayer("mapbox.streets");
    mapView.setTileSource(mbTileLayer);

    box = mbTileLayer.getBoundingBox();
    mapView.setScrollableAreaLimit(box);
    mapView.setMinZoomLevel(mapView.getTileProvider().getMinimumZoomLevel());
    mapView.setMaxZoomLevel(mapView.getTileProvider().getMaximumZoomLevel());
    mapView.setCenter(mapView.getTileProvider().getCenterCoordinate());
    mapView.setZoom(0);
    Log.d("MainActivity", "zoomToBoundingBox " + box.toString());
    setContentView(mapView);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    Resources res = this.getResources();
    int resId = res.getIdentifier("map_editor_activity_actions", "menu", this.getPackageName());

    // Inflate the menu items for use in the action bar
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(resId, menu);
    return super.onCreateOptionsMenu(menu);
  }

}
