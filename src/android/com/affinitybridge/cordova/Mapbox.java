package com.affinitybridge.cordova.mapbox;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import android.util.Log;
import android.app.Activity;
import android.content.res.Resources;

import com.mapbox.mapboxsdk.views.MapView;
import com.mapbox.mapboxsdk.tileprovider.tilesource.MapboxTileLayer;

public class Mapbox extends CordovaPlugin {

  public static final String ACTION_CREATE_MAP = "createMap";

  public static final String TILELAYER_MAPBOX_STREETS = "mapbox.streets";

  @Override
  public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
    try {
      if (ACTION_CREATE_MAP.equals(action)) {
        createMap();
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

  private void createMap() {
    cordova.getActivity().runOnUiThread(new Runnable() {
      public void run() {
        Activity activity = cordova.getActivity();
        Resources res = activity.getResources();

        // Get Mapbox access token from Android's application resources.
        int resId = res.getIdentifier("mapboxAccessToken", "string", activity.getPackageName());
        String accessToken = res.getString(resId);

        MapView mapView = new MapView(activity);
        mapView.setAccessToken(accessToken);
        mapView.setTileSource(new MapboxTileLayer(TILELAYER_MAPBOX_STREETS));
        activity.setContentView(mapView);
      }
    });
  }

}
