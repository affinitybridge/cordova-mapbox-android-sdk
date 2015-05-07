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
    Intent intent = new Intent(cordova.getActivity(), OfflineMapActivity.class);
    intent.putExtra(Intent.EXTRA_TEXT, fileName);
    cordova.getActivity().startActivity(intent);
  }

  private void mapEditor() {
    sendMessage();
  }

  private void sendMessage() {
    /* this.cordova.setActivityResultCallback(this); */
    Intent intent = new Intent(cordova.getActivity(), MapEditorActivity.class);
    intent.putExtra(Intent.EXTRA_TEXT, "" +
//      "{\n" +
//      "  \"type\": \"FeatureCollection\",\n" +
//      "  \"features\": [\n" +
//      "    {\n" +
//      "      \"type\": \"Feature\",\n" +
//      "      \"properties\": {},\n" +
//      "      \"geometry\": {\n" +
//      "        \"type\": \"LineString\",\n" +
//      "        \"coordinates\": [\n" +
//      "          [\n" +
//      "            -123.0904769897461,\n" +
//      "            49.32019889132852\n" +
//      "          ],\n" +
//      "          [\n" +
//      "            -123.13510894775389,\n" +
//      "            49.31997510237421\n" +
//      "          ],\n" +
//      "          [\n" +
//      "            -123.17321777343749,\n" +
//      "            49.316618146025625\n" +
//      "          ],\n" +
//      "          [\n" +
//      "            -123.24874877929686,\n" +
//      "            49.279004601275915\n" +
//      "          ],\n" +
//      "          [\n" +
//      "            -123.10901641845703,\n" +
//      "            49.24405203271575\n" +
//      "          ],\n" +
//      "          [\n" +
//      "            -123.17768096923827,\n" +
//      "            49.28863461100519\n" +
//      "          ],\n" +
//      "          [\n" +
//      "            -123.11450958251953,\n" +
//      "            49.28012446647762\n" +
//      "          ],\n" +
//      "          [\n" +
//      "            -123.07468414306642,\n" +
//      "            49.2566019569463\n" +
//      "          ],\n" +
//      "          [\n" +
//      "            -123.08876037597656,\n" +
//      "            49.2933369555088\n" +
//      "          ],\n" +
//      "          [\n" +
//      "            -123.04962158203124,\n" +
//      "            49.30408348724266\n" +
//      "          ],\n" +
//      "          [\n" +
//      "            -123.10935974121092,\n" +
//      "            49.30542663894831\n" +
//      "          ]\n" +
//      "        ]\n" +
//      "      }\n" +
//      "    }," +
//      "    {\n" +
//      "      \"type\": \"Feature\",\n" +
//      "      \"properties\": {},\n" +
//      "      \"geometry\": {\n" +
//      "        \"type\": \"Polygon\",\n" +
//      "        \"coordinates\": [\n" +
//      "          [\n" +
//      "            [\n" +
//      "              -123.13622474670409,\n" +
//      "              49.276260824062035\n" +
//      "            ],\n" +
//      "            [\n" +
//      "              -123.14051628112793,\n" +
//      "              49.27978850958716\n" +
//      "            ],\n" +
//      "            [\n" +
//      "              -123.14437866210939,\n" +
//      "              49.28443571009498\n" +
//      "            ],\n" +
//      "            [\n" +
//      "              -123.14403533935547,\n" +
//      "              49.28863461100519\n" +
//      "            ],\n" +
//      "            [\n" +
//      "              -123.14995765686035,\n" +
//      "              49.291881515854996\n" +
//      "            ],\n" +
//      "            [\n" +
//      "              -123.15330505371094,\n" +
//      "              49.295576009528354\n" +
//      "            ],\n" +
//      "            [\n" +
//      "              -123.15879821777342,\n" +
//      "              49.298262740098366\n" +
//      "            ],\n" +
//      "            [\n" +
//      "              -123.15948486328126,\n" +
//      "              49.301285136928556\n" +
//      "            ],\n" +
//      "            [\n" +
//      "              -123.15622329711915,\n" +
//      "              49.30839263593014\n" +
//      "            ],\n" +
//      "            [\n" +
//      "              -123.15176010131836,\n" +
//      "              49.31225376069211\n" +
//      "            ],\n" +
//      "            [\n" +
//      "              -123.14154624938963,\n" +
//      "              49.31426814043642\n" +
//      "            ],\n" +
//      "            [\n" +
//      "              -123.13502311706542,\n" +
//      "              49.307049565077236\n" +
//      "            ],\n" +
//      "            [\n" +
//      "              -123.12970161437987,\n" +
//      "              49.30290819946615\n" +
//      "            ],\n" +
//      "            [\n" +
//      "              -123.12661170959474,\n" +
//      "              49.3026283649115\n" +
//      "            ],\n" +
//      "            [\n" +
//      "              -123.11948776245117,\n" +
//      "              49.29966202093265\n" +
//      "            ],\n" +
//      "            [\n" +
//      "              -123.11717033386229,\n" +
//      "              49.30078141699555\n" +
//      "            ],\n" +
//      "            [\n" +
//      "              -123.1168270111084,\n" +
//      "              49.29837468402711\n" +
//      "            ],\n" +
//      "            [\n" +
//      "              -123.12137603759767,\n" +
//      "              49.295576009528354\n" +
//      "            ],\n" +
//      "            [\n" +
//      "              -123.12317848205565,\n" +
//      "              49.29137769980898\n" +
//      "            ],\n" +
//      "            [\n" +
//      "              -123.11759948730469,\n" +
//      "              49.290426033229025\n" +
//      "            ],\n" +
//      "            [\n" +
//      "              -123.11038970947266,\n" +
//      "              49.28930640196742\n" +
//      "            ],\n" +
//      "            [\n" +
//      "              -123.10721397399902,\n" +
//      "              49.28981023918181\n" +
//      "            ],\n" +
//      "            [\n" +
//      "              -123.10309410095215,\n" +
//      "              49.2863392561493\n" +
//      "            ],\n" +
//      "            [\n" +
//      "              -123.09991836547852,\n" +
//      "              49.28572341105575\n" +
//      "            ],\n" +
//      "            [\n" +
//      "              -123.10146331787108,\n" +
//      "              49.25705010952243\n" +
//      "            ],\n" +
//      "            [\n" +
//      "              -123.14180374145508,\n" +
//      "              49.25716214703067\n" +
//      "            ],\n" +
//      "            [\n" +
//      "              -123.13622474670409,\n" +
//      "              49.276260824062035\n" +
//      "            ]\n" +
//      "          ]\n" +
//      "        ]\n" +
//      "      }\n" +
//      "    }\n" +
//      "  ]\n" +
//      "}" +
    "");
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
