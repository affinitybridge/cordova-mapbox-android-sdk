package com.affinitybridge.cordova.mapbox;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;

import org.apache.cordova.CordovaResourceApi;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.net.Uri;
import android.util.Log;
import android.app.Activity;
import android.content.res.Resources;
import android.content.Intent;

import com.cocoahero.android.geojson.GeoJSON;
import com.cocoahero.android.geojson.GeoJSONObject;
import com.mapbox.mapboxsdk.geometry.BoundingBox;
import com.mapbox.mapboxsdk.views.MapView;
import com.mapbox.mapboxsdk.tileprovider.tilesource.ITileLayer;
import com.mapbox.mapboxsdk.tileprovider.tilesource.TileLayer;
import com.mapbox.mapboxsdk.tileprovider.tilesource.MapboxTileLayer;
import com.mapbox.mapboxsdk.tileprovider.tilesource.MBTilesLayer;
import com.mapbox.mapboxsdk.tileprovider.tilesource.WebSourceTileLayer;

import java.io.File;

public class Mapbox extends CordovaPlugin {

  public static final String ACTION_CREATE_MAPBOX_MAP = "createMapboxTileLayerMap";
  public static final String ACTION_CREATE_MBTILES_MAP = "createMBTilesLayerMap";
  public static final String ACTION_MAP_EDITOR = "mapEditor";

  public static final String EXTRA_GEOJSON = "extraGeoJSON";
  public static final String EXTRA_MBTILES = "extraMbtiles";
  public static final String EXTRA_MAPID = "extraMapId";

  protected CallbackContext activeCallbackContext;

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
        JSONObject options = args.getJSONObject(0);
        this.activeCallbackContext = callbackContext;
        mapEditor(options);
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
    Intent intent = new Intent(cordova.getActivity(), OfflineMapActivity.class);
    intent.putExtra(Intent.EXTRA_TEXT, fileName);
    cordova.getActivity().startActivity(intent);
  }

  private void mapEditor(JSONObject options) {
    Intent intent = new Intent(cordova.getActivity(), MapEditorActivity.class);
    String errorMessage;

    try {
      if (options.has("geojson")) {
        GeoJSONObject geojson = GeoJSON.parse(options.getJSONObject("geojson"));
        intent.putExtra(Mapbox.EXTRA_GEOJSON, geojson);
      }

      if (options.has("mbtiles")) {

        CordovaResourceApi resourceApi = webView.getResourceApi();
        String mbtilesPath = options.getString("mbtiles");
        // Accept a path or a URI for the source.
        Uri tmpSrc = Uri.parse(mbtilesPath);
        final Uri mbtilesUri = resourceApi.remapUri(
                tmpSrc.getScheme() != null ? tmpSrc : Uri.fromFile(new File(mbtilesPath)));
        intent.putExtra(Mapbox.EXTRA_MBTILES, mbtilesUri);
      } else if (options.has("mapId")) {
        intent.putExtra(Mapbox.EXTRA_MAPID, options.getString("mapId"));
      } else {
        errorMessage =  "A basemap must be specified with a 'mbtiles' or 'mapId' property in mapEditor options.";
        Log.e("Mapbox", errorMessage);
        this.activeCallbackContext.error(errorMessage);
        return;
      }
    }
    catch (JSONException e) {
       Log.e("mapEditor()", e.getMessage());
       this.activeCallbackContext.error(e.getMessage());
       return;
    }

    /* this.cordova.setActivityResultCallback(this); */
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
          try {
            Log.d("MapboxPlugin", "Intent extra: " + features);
            this.activeCallbackContext.success(new JSONObject(features));
          }
          catch (JSONException e) {
            Log.e("Mapbox", e.getMessage());
            this.activeCallbackContext.error(e.getMessage());
          }
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
