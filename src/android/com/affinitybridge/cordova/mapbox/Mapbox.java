package com.affinitybridge.cordova.mapbox;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.app.Activity;
import android.content.res.Resources;
import android.content.Intent;

import com.cocoahero.android.geojson.Feature;
import com.cocoahero.android.geojson.FeatureCollection;
import com.cocoahero.android.geojson.GeoJSON;
import com.cocoahero.android.geojson.GeoJSONObject;
import com.cocoahero.android.geojson.Geometry;
import com.cocoahero.android.geojson.LineString;
import com.cocoahero.android.geojson.MultiLineString;
import com.cocoahero.android.geojson.MultiPoint;
import com.cocoahero.android.geojson.MultiPolygon;
import com.cocoahero.android.geojson.Point;
import com.cocoahero.android.geojson.Polygon;
import com.cocoahero.android.geojson.Position;
import com.cocoahero.android.geojson.Ring;
import com.mapbox.mapboxsdk.geometry.BoundingBox;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.util.GeoUtils;
import com.mapbox.mapboxsdk.views.MapView;
import com.mapbox.mapboxsdk.tileprovider.tilesource.TileLayer;
import com.mapbox.mapboxsdk.tileprovider.tilesource.MapboxTileLayer;
import com.mapbox.mapboxsdk.views.util.TilesLoadedListener;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class Mapbox extends CordovaPlugin {

  public static final String ACTION_CREATE_MAPBOX_MAP = "createMapboxTileLayerMap";
  public static final String ACTION_CREATE_MBTILES_MAP = "createMBTilesLayerMap";
  public static final String ACTION_MAP_EDITOR = "mapEditor";
  public static final String ACTION_CREATE_STATIC_IMAGE = "createStaticImage";

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
        String geojson = args.getString(0);
        this.activeCallbackContext = callbackContext;
        mapEditor(geojson);
        return true;
      }
      else if (ACTION_CREATE_STATIC_IMAGE.equals(action)) {
        JSONObject options = args.getJSONObject(0);
        this.activeCallbackContext = callbackContext;
        createStaticImage(options);
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

  private void mapEditor(String geojson) {
    /* this.cordova.setActivityResultCallback(this); */
    Intent intent = new Intent(cordova.getActivity(), MapEditorActivity.class);
    intent.putExtra(Intent.EXTRA_TEXT, geojson);
    cordova.startActivityForResult(this, intent, 1);
  }

  private void createStaticImage(JSONObject options) {
    final Bitmap bm;
    final Canvas canvas;
    int width = 100, height = 100;

    GeoJSONObject geojson;
    ArrayList<LatLng> latlngs = new ArrayList<LatLng>();
    BoundingBox focusArea;

    try {
      if (options.has("height")) {
        height = options.getInt("height");
      }

      if (options.has("width")) {
        width = options.getInt("width");
      }

      if (options.has("geojson")) {
        geojson = GeoJSON.parse(options.getJSONObject("geojson"));
        this.getLatLngs(geojson, latlngs);
        if (latlngs.isEmpty()) {
          Log.e("createStaticImage()", "Empty 'geojson' property in createStaticImage() options.");
          return;
        }

        focusArea = GeoUtils.findBoundingBoxForGivenLocations(latlngs, 2.5);
        double west = focusArea.getLonWest(), north = focusArea.getLatNorth(), east = focusArea.getLonEast(), south = focusArea.getLatSouth();
        String bbox = String.format("[[ [%f,%f], [%f,%f], [%f,%f], [%f,%f], [%f,%f] ]]", west, north, east, north, east, south, west, south, west, north);
        Log.d("createStaticImage()", String.format("LatLngs: %s, focusArea: %s, BBOX: %s", latlngs.toString(), focusArea.toString(), bbox));

      } else {
        Log.e("createStaticImage()", "Missing 'geojson' property in createStaticImage() options.");
        return;
      }

      createStaticImage(width, height, focusArea);
    }
    catch (JSONException e) {
      Log.e("createStaticImage()", e.getMessage());
      this.activeCallbackContext.error(e.getMessage());
      return;
    }
  }

  protected void createStaticImage(final int width, final int height, final BoundingBox focusArea) {
    final Bitmap bm = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
    final Canvas canvas = new Canvas(bm);

    Activity activity = cordova.getActivity();
    Resources res = activity.getResources();
    int accessTokenID = res.getIdentifier("mapboxAccessToken", "string", activity.getPackageName());
    String accessToken = res.getString(accessTokenID);

    final MapView mapView = new MapView(webView.getContext());
    mapView.setAccessToken(accessToken);
    TileLayer mbTileLayer = new MapboxTileLayer("affinitybridge.map-yxek1zgp");
    mapView.setTileSource(mbTileLayer);
    mapView.setCenter(focusArea.getCenter());
    mapView.zoomToBoundingBox(focusArea, true, false, true);

    Log.d("TilesLoadedListener", "Calling mapView.draw() to trigger tile loading.");
    mapView.draw(canvas);

    mapView.setOnTilesLoadedListener(new TilesLoadedListener() {
      @Override
      public boolean onTilesLoaded() {
        cordova.getActivity().runOnUiThread(new Runnable() {
          public void run() {
            Log.d("TilesLoadedListener", String.format("onTilesLoaded() mapView.getCenter() %s", mapView.getCenter().toString()));
            mapView.draw(canvas);
            Uri uri = writeImage(bm);
            activeCallbackContext.success(uri.toString());
          }
        });
        return true;
      }

      @Override
      public boolean onTilesLoadStarted() {
        return false;
      }
    });
  }

  public Uri writeImage(Bitmap bitmap) {
    String errorMessage;
    int quality = 80;
    Uri uri = Uri.fromFile(new File(getTempDirectoryPath(), System.currentTimeMillis() + ".jpg"));

    try {
      OutputStream os = this.cordova.getActivity().getContentResolver().openOutputStream(uri);
      bitmap.compress(Bitmap.CompressFormat.JPEG, quality, os);
      os.close();
    } catch (FileNotFoundException e) {
      errorMessage = e.getMessage();
      Log.e("Mapbox", errorMessage);
      this.activeCallbackContext.error(errorMessage);
    } catch (IOException e) {
      errorMessage = e.getMessage();
      Log.e("Mapbox", errorMessage);
      this.activeCallbackContext.error(errorMessage);
    }

    return uri;
  }

  private String getTempDirectoryPath() {
    File cache = null;

    // SD Card Mounted
    if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
      cache = new File(Environment.getExternalStorageDirectory().getAbsolutePath() +
              "/Android/data/" + cordova.getActivity().getPackageName() + "/cache/");
    }
    // Use internal storage
    else {
      cache = cordova.getActivity().getCacheDir();
    }

    // Create the cache directory if it doesn't exist
    cache.mkdirs();
    return cache.getAbsolutePath();
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

  /**
   * TODO: These getLatLngs() methods should exist under a 'utils' namespace.
   *
   * Fills latlngs collection with all latlngs contained within geojson.
   *
   * @param geojson
   * @param latlngs
   */
  public void getLatLngs(GeoJSONObject geojson, ArrayList<LatLng> latlngs) {
    Log.d("Mapbox.java", "getLatLngs(GeoJSONObject)");
    Log.d("Mapbox.java", String.format("Type: %s.", geojson.getType()));

    if (geojson instanceof FeatureCollection) {
      FeatureCollection fc = (FeatureCollection) geojson;
      for (Feature f : fc.getFeatures()) {
        this.getLatLngs(f, latlngs);
      }
    }
    else if (geojson instanceof Feature) {
      Log.d("Builder", "instanceof Feature");
      Feature feature = (Feature) geojson;
      this.getLatLngs(feature.getGeometry(), latlngs);
    }
    else {
      Log.w("Mapbox.java", "GeoJSONObject must be either instance of Feature or FeatureCollection.");
    }
  }

  /**
   * Fills latlngs collection with all latlngs contained within geom.
   *
   * @param geom
   * @param latlngs
   */
  protected void getLatLngs(Geometry geom, ArrayList<LatLng> latlngs) {
    if (geom instanceof MultiPolygon) {
      for (Polygon poly : ((MultiPolygon) geom).getPolygons()) {
        this.getLatLngs(poly, latlngs);
      }
    } else if (geom instanceof MultiLineString) {
      for (LineString line : ((MultiLineString) geom).getLineStrings()) {
        this.getLatLngs(line, latlngs);
      }
    } else if (geom instanceof MultiPoint) {
      this.getLatLngs(((MultiPoint) geom).getPositions(), latlngs);
    } else if (geom instanceof Polygon) {
      for (Ring r : ((Polygon) geom).getRings()) {
        this.getLatLngs(r.getPositions(), latlngs);
      }
    } else if (geom instanceof LineString) {
      this.getLatLngs(((LineString) geom).getPositions(), latlngs);
    } else if (geom instanceof Point) {
      latlngs.add(this.getLatLng(((Point) geom).getPosition()));
    }
    else {
      Log.w("Mapbox.java", String.format("Unknown geometry type: %s", geom.getType()));
    }
  }

  /**
   * Fills latlngs with all latlngs within positions.
   *
   * @param positions
   * @param latlngs
   */
  protected void getLatLngs(List<Position> positions, ArrayList<LatLng> latlngs) {
    for (Position point : positions) {
      latlngs.add(this.getLatLng(point));
    }
  }

  /**
   * Converts a Position to a LatLng.
   *
   * @param position
   * @return A new LatLng representation of the Position.
   */
  protected LatLng getLatLng(Position position) {
    return new LatLng(position.getLatitude(), position.getLongitude());
  }

}
