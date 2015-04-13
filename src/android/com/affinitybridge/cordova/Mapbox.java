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
  public static final String ACTION_ECHO = "echo";

  public static final String AB_TILELAYER_OFFLINE = "mapbox.streets";

  @Override
  public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
    try {
      if (ACTION_CREATE_MAP.equals(action)) {
        createMap();
        return true;
      }
      else if (ACTION_ECHO.equals(action)) {
        String message = args.getString(0);
        this.echo(message, callbackContext);
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

  private void echo(String message, CallbackContext callbackContext) {
    if (message != null && message.length() > 0) {
      callbackContext.success(message);
    }
    else {
      callbackContext.error("Expected one non-empty string argument.");
    }
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
        mapView.setTileSource(new MapboxTileLayer(AB_TILELAYER_OFFLINE));
        activity.setContentView(mapView);
      }
    });
  }
}
