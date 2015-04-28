package com.affinitybridge.cordova.mapbox;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;

import org.json.JSONArray;
import org.json.JSONException;

import android.util.Log;
import android.app.Activity;
import android.content.res.Resources;
import android.content.Intent;

import com.mapbox.mapboxsdk.geometry.BoundingBox;
import com.mapbox.mapboxsdk.views.MapView;
import com.mapbox.mapboxsdk.tileprovider.tilesource.ITileLayer;
import com.mapbox.mapboxsdk.tileprovider.tilesource.TileLayer;
import com.mapbox.mapboxsdk.tileprovider.tilesource.MapboxTileLayer;
import com.mapbox.mapboxsdk.tileprovider.tilesource.MBTilesLayer;
import com.mapbox.mapboxsdk.tileprovider.tilesource.WebSourceTileLayer;

public class Mapbox extends CordovaPlugin {

  public static final String ACTION_CREATE_MAPBOX_MAP = "createMapboxTileLayerMap";
  public static final String ACTION_CREATE_MBTILES_MAP = "createMBTilesLayerMap";
  public static final String ACTION_MAP_EDITOR = "mapEditor";

  @Override
  public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
    try {
      if (ACTION_CREATE_MAPBOX_MAP.equals(action)) {
        String mapId = args.getString(0);
        if (mapId != null && mapId.length() > 0) {
          createMapboxMap(mapId);
          callbackContext.success("Mapbox Map created.");
          return true;
        }
      }
      else if (ACTION_CREATE_MBTILES_MAP.equals(action)) {
        String fileName = args.getString(0);
        if (fileName != null && fileName.length() > 0) {
          createMBTilesMap(fileName);
          callbackContext.success("MBTiles Map created.");
          return true;
        }
      }
      else if (ACTION_MAP_EDITOR.equals(action)) {
        mapEditor();
        callbackContext.success("Map Editor started...");
        return true;
      }
      callbackContext.error("Invalid action");
    }
    catch(Exception e) {
      System.err.println("Exception: " + e.getMessage());
      callbackContext.error(e.getMessage());
    }
    return false;
  }

  private void createMapboxMap(final String mapId) {

    this.cordova.getActivity().runOnUiThread(new Runnable() {
      public void run() {
        Activity activity = cordova.getActivity();
        MapView mapView = new MapView(webView.getContext());
        BoundingBox box;

        // Get Mapbox access token from Android's application resources.
        Resources res = activity.getResources();
        int resId = res.getIdentifier("mapboxAccessToken", "string", activity.getPackageName());
        String accessToken = res.getString(resId);

        // Mapbox tile layer.
        mapView.setAccessToken(accessToken);
        TileLayer mbTileLayer = new MapboxTileLayer(mapId);
        mapView.setTileSource(mbTileLayer);
        // END Mapbox tile layer.

        box = mbTileLayer.getBoundingBox();
        mapView.setScrollableAreaLimit(box);
        mapView.setMinZoomLevel(mapView.getTileProvider().getMinimumZoomLevel());
        mapView.setMaxZoomLevel(mapView.getTileProvider().getMaximumZoomLevel());
        mapView.setCenter(mapView.getTileProvider().getCenterCoordinate());
        mapView.setZoom(0);
        Log.d("MapboxPlugin", "zoomToBoundingBox " + box.toString());
        activity.setContentView(mapView);
      }
    });
  }

  private void createMBTilesMap(final String fileName) {
    this.cordova.getActivity().runOnUiThread(new Runnable() {
      public void run() {
        Activity activity = cordova.getActivity();
        MapView mapView = new MapView(webView.getContext());
        BoundingBox box;

        TileLayer mbTileLayer = new MBTilesLayer(activity, fileName);
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
        activity.setContentView(mapView);
      }
    });
  }

  private void mapEditor() {
    sendMessage();
  }

  private void sendMessage() {
    /* this.cordova.setActivityResultCallback(this); */
    Intent intent = new Intent(cordova.getActivity(), MapEditorActivity.class);
    cordova.startActivityForResult(this, intent, 1);
  }

  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    Log.d("MapboxPlugin", "onActivityResult() called.");
    if (requestCode == 1) {
      Log.d("MapboxPlugin", "onActivityResult() request code == 1.");
      if (resultCode == cordova.getActivity().RESULT_OK) {
        Log.d("MapboxPlugin", "Activity returned RESULT_OK.");
        if (data != null) {
          String features = data.getStringExtra(Intent.EXTRA_TEXT);
          Log.d("MapboxPlugin", "Intent extra: " + features);
        }
        else {
          Log.d("MapboxPlugin", "Intent is null");
        }
      }
      else if (resultCode == cordova.getActivity().RESULT_CANCELED) {
        //String geometries = data.getData();
        Log.d("MapboxPlugin", "Activity returned RESULT_CENCELED.");
      }
    }
  }

}
