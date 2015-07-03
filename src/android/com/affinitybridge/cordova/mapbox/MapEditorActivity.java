package com.affinitybridge.cordova.mapbox;

import android.content.res.Resources;
import android.database.sqlite.SQLiteCantOpenDatabaseException;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.ActionMode;
import android.view.MenuInflater;
import android.view.Menu;
import android.view.MenuItem;

import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.cocoahero.android.geojson.Feature;
import com.cocoahero.android.geojson.FeatureCollection;
import com.cocoahero.android.geojson.GeoJSONObject;
import com.cocoahero.android.geojson.Geometry;
import com.cocoahero.android.geojson.GeometryCollection;
import com.cocoahero.android.geojson.LineString;
import com.cocoahero.android.geojson.MultiLineString;
import com.cocoahero.android.geojson.MultiPoint;
import com.cocoahero.android.geojson.MultiPolygon;
import com.cocoahero.android.geojson.Point;
import com.cocoahero.android.geojson.Polygon;
import com.cocoahero.android.geojson.Position;
import com.cocoahero.android.geojson.Ring;
import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;
import com.mapbox.mapboxsdk.geometry.BoundingBox;
import com.mapbox.mapboxsdk.geometry.LatLng;

import com.mapbox.mapboxsdk.tileprovider.tilesource.MBTilesLayer;
import com.mapbox.mapboxsdk.tileprovider.tilesource.MapboxTileLayer;
import com.mapbox.mapboxsdk.tileprovider.tilesource.TileLayer;
import com.mapbox.mapboxsdk.util.GeoUtils;
import com.mapbox.mapboxsdk.views.MapView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class MapEditorActivity extends Activity {

  protected MapView mapView;

  protected boolean userLocationEnabled = false;

  protected BoundingBox mapExtent;

  protected Builder featureBuilder;

  protected ActionMode mActionMode;

  protected FloatingActionsMenu addMenu;

  protected FloatingActionButton getLocation;

  protected FloatingActionButton addVertex;

  protected ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {

    // Called when the action mode is created; startActionMode() was called
    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
      Log.d("ActionMode.Callback", "onCreateActionMode()");
      // Inflate a menu resource providing context menu items
      MenuInflater inflater = mode.getMenuInflater();
      inflater.inflate(resource("menu", "context_menu_add_feature"), menu);
      return true;
    }

    // Called each time the action mode is shown. Always called after onCreateActionMode, but
    // may be called multiple times if the mode is invalidated.
    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
      Log.d("ActionMode.Callback", "onPrepareActionMode()");
      return false; // Return false if nothing is done
    }

    // Called when the user selects a contextual menu item
    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
      Log.d("ActionMode.Callback", "onActionItemClicked()");
      if (item.getItemId() == resource("id", "action_done")) {
        mode.finish(); // Action picked, so close the CAB
        return true;
      } else {
        return false;
      }
    }

    // Called when the user exits the action mode
    @Override
    public void onDestroyActionMode(ActionMode mode) {
      Log.d("ActionMode.Callback", "onDestroyActionMode()");
      featureBuilder.stopFeature();
      mActionMode = null;
      addVertex.setVisibility(View.INVISIBLE);
      addMenu.setVisibility(View.VISIBLE);
    }
  };

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    this.setContentView(this.resource("layout", "map_editor"));

    MapView mapView = this.mapView = (MapView) this.findViewById(this.resource("id", "mapeditor"));

    Intent intent = this.getIntent();
    this.initFeatureBuilder(mapView);
    this.initGeometryTypeLimits(intent);
    this.initAddMenuButtons();
    this.initBaseLayer(intent, mapView);

    ArrayList<LatLng> latLngs = new ArrayList<LatLng>();

    if (intent.hasExtra(Mapbox.EXTRA_GEOJSON)) {
      GeoJSONObject geojson = intent.getParcelableExtra(Mapbox.EXTRA_GEOJSON);
      this.parseGeoJSON(geojson, latLngs);
      BoundingBox featuresExtent = GeoUtils.findBoundingBoxForGivenLocations(latLngs, 1.0);
      Log.d("MapEditorActivity", String.format("Number of latlngs: %d", latLngs.size()));

      if (MapEditorActivity.pointsWithinBox(this.mapExtent, latLngs)) {
        Log.d("MapEditorActivity", String.format("Zooming to Features extent: %s", featuresExtent));
        mapView.zoomToBoundingBox(featuresExtent);
        mapView.setCenter(featuresExtent.getCenter());
      } else if (latLngs.size() > 0) {
        CharSequence message = "Some features are outside of the available map area.";
        Toast toast = Toast.makeText(this.getBaseContext(), message, Toast.LENGTH_LONG);
        toast.show();
        Log.d("MapEditorActivity", message.toString());
      }
    }

    if (latLngs.isEmpty()) {
      Log.d("MapEditorActivity", "No GeoJSON, zooming to user location.");
      this.toggleUserLocation(true);
    }
  }

  protected void initFeatureBuilder(MapView map) {
    Resources res = this.getResources();
    GradientDrawable vertexImg = (GradientDrawable) res.getDrawable(this.resource("drawable", "vertex_marker"));

    // Pre-rendering images for the three states of vertex marker; default, selected and ghost.
    vertexImg.setColor(Color.parseColor("#FFFFFF"));
    Drawable vertexImage = new BitmapDrawable(res, MapEditorActivity.toBitmap(vertexImg));
    vertexImg.setAlpha(60);
    Drawable vertexMiddleImage = new BitmapDrawable(res, MapEditorActivity.toBitmap(vertexImg));
    vertexImg.setAlpha(255);
    vertexImg.setColor(Color.parseColor("#FFFF00"));
    Drawable vertexSelectedImage = new BitmapDrawable(res, MapEditorActivity.toBitmap(vertexImg));

    this.featureBuilder = new Builder(map);
    this.featureBuilder.setVertexImage(vertexImage);
    this.featureBuilder.setVertexMiddleImage(vertexMiddleImage);
    this.featureBuilder.setVertexSelectedImage(vertexSelectedImage);
  }

  protected void initAddMenuButtons() {
    this.addMenu = (FloatingActionsMenu) this.findViewById(this.resource("id", "actions_add"));
    this.addVertex = (FloatingActionButton) this.findViewById(this.resource("id", "action_add_vertex"));
    this.getLocation = (FloatingActionButton) this.findViewById(this.resource("id", "action_get_location"));

    this.getLocation.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        toggleUserLocation();
      }
    });

    this.addVertex.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        featureBuilder.addLatLng();
      }
    });

    if (this.featureBuilder.getMaxPoints() != 0) {
      FloatingActionButton addPoint = new FloatingActionButton(this.getBaseContext());
      addPoint.setTitle("Add point");
      addPoint.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          addFeatureOnClick(featureBuilder.createPoint());
        }
      });
      addMenu.addButton(addPoint);
    }

    if (this.featureBuilder.getMaxLines() != 0) {
      FloatingActionButton addLine = new FloatingActionButton(this.getBaseContext());
      addLine.setTitle("Add line");
      addLine.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          addFeatureOnClick(featureBuilder.createLineString());
        }
      });
      addMenu.addButton(addLine);
    }

    if (this.featureBuilder.getMaxPolygons() != 0) {
      FloatingActionButton addPoly = new FloatingActionButton(this.getBaseContext());
      addPoly.setTitle("Add polygon");
      addPoly.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          addFeatureOnClick(featureBuilder.createPolygon());
        }
      });
      addMenu.addButton(addPoly);
    }
  }

  public void initBaseLayer(Intent intent, MapView map) {
    TileLayer base = null;

    if (intent.hasExtra(Mapbox.EXTRA_MBTILES)) {
      Uri uri = intent.getParcelableExtra(Mapbox.EXTRA_MBTILES);
      try {
        base = new MBTilesLayer(uri.getPath());
      }
      catch (SQLiteCantOpenDatabaseException e) {
        Log.e("MapEditorActivity", e.getMessage());
        return;
      }
    } else if (intent.hasExtra(Mapbox.EXTRA_MAPID)) {
      String mapId = intent.getStringExtra(Mapbox.EXTRA_MAPID);
      base = new MapboxTileLayer(mapId);
    }

    if (base == null) {
      Log.e("MapEditorActivity", "No basemap provided (mapId or mbtiles).");
      return;
    }

    // Halfway between max and min zoom levels.
    float min = base.getMinimumZoomLevel(), max = base.getMaximumZoomLevel();
    float zoom = min + ((max - min) / 2);
//    zoom = base.getMinimumZoomLevel();
//    zoom = 9.0f;

    this.mapExtent = base.getBoundingBox();
    map.setTileSource(base);
    map.setScrollableAreaLimit(this.mapExtent);
    map.setMinZoomLevel(base.getMinimumZoomLevel());
    map.setMaxZoomLevel(base.getMaximumZoomLevel());
    map.setCenter(base.getCenterCoordinate());
    map.setZoom(zoom);

    Log.d("MapEditorActivity", String.format("extent (%s), center (%s), zoom: (%f), minZoom: (%f), maxZoom: (%f)", base.getBoundingBox(), base.getCenterCoordinate(), zoom, base.getMinimumZoomLevel(), base.getMaximumZoomLevel()));
  }

  protected void initGeometryTypeLimits(Intent intent) {
    if (intent.hasExtra(Mapbox.EXTRA_GEOMETRY_TYPES)) {
      try {
        JSONObject types = new JSONObject(intent.getStringExtra(Mapbox.EXTRA_GEOMETRY_TYPES));
        this.featureBuilder.setLimits(types);
      } catch (JSONException e) {
        Log.e("MapEditorActivity", e.getMessage());
      }
    }
  }

  public void addFeatureOnClick(Builder.GeometryInterface geometry) {
    if (this.featureBuilder.startFeature(geometry)) {
      this.addMenu.collapse();
      this.addMenu.setVisibility(View.INVISIBLE);
      this.addVertex.setVisibility(View.VISIBLE);
      this.mActionMode = startActionMode(mActionModeCallback);
    } else {
      CharSequence message;
      if (geometry instanceof PointGeometry) {
        message = "Maximum number of points reached.";
      } else if (geometry instanceof LineGeometry) {
        message = "Maximum number of lines reached.";
      } else if (geometry instanceof PolygonGeometry) {
        message = "Maximum number of polygons reached.";
      } else {
        message = "Unable to add another geometry.";
      }
      Toast.makeText(getBaseContext(), message, Toast.LENGTH_LONG).show();
    }
  }

  public void toggleUserLocation() {
    this.toggleUserLocation(!this.userLocationEnabled);
  }

  public void toggleUserLocation(boolean on) {
    this.userLocationEnabled = on;
    this.mapView.setUserLocationEnabled(on);
    Log.d("MapEditorActivity", String.format("User location enabled: %b.", this.userLocationEnabled));

    if (on) {
      LatLng userLocation = this.mapView.getUserLocation();
      if (this.mapExtent.contains(userLocation)) {
        this.mapView.setCenter(userLocation);
      } else {
        CharSequence message = "User location is outside of available map area.";
        Toast.makeText(this.getBaseContext(), message, Toast.LENGTH_LONG).show();
        this.toggleUserLocation(false);
      }
    }
  }

  @Override
  public boolean onNavigateUp() {
    Intent intent = new Intent();
    intent.putExtra(Intent.EXTRA_TEXT, this.featureBuilder.toJSON().toString());
    this.setResult(this.RESULT_OK, intent);

    return super.onNavigateUp();
  }

  private int resource(String type, String name) {
    // TODO: This isn't very pretty but I don't know of an alternative.
    Resources res = this.getResources();
    String packageName = this.getPackageName();
    return res.getIdentifier(name, type, packageName);
  }

  public ArrayList<LatLng> parseGeoJSON(GeoJSONObject geojson, ArrayList<LatLng> latLngs) {
    if (geojson instanceof FeatureCollection) {
      FeatureCollection fc = (FeatureCollection) geojson;
      for (Feature f : fc.getFeatures()) {
        latLngs.addAll(this.parseGeoJSON(f, new ArrayList<LatLng>()));
      }
    } else if (geojson instanceof Feature) {
      Feature feature = (Feature) geojson;
      latLngs.addAll(drawGeometry(feature.getGeometry(), new ArrayList<LatLng>()));
    } else {
      Log.d("Builder", "Not instanceof Feature.");
    }

    return latLngs;
  }

  public ArrayList<LatLng> drawGeometry(Geometry geom, ArrayList<LatLng> latLngs) {
    if (geom instanceof GeometryCollection) {
      GeometryCollection gc = (GeometryCollection) geom;
      for (Geometry g : gc.getGeometries()) {
        latLngs.addAll(this.drawGeometry(g, new ArrayList<LatLng>()));
      }
    }
    else if (geom instanceof MultiPolygon) {
      MultiPolygon multiPoly = (MultiPolygon) geom;
      for (Polygon poly : multiPoly.getPolygons()) {
        latLngs.addAll(this.drawGeometry(poly, new ArrayList<LatLng>()));
      }
    }
    if (geom instanceof Polygon) {
      Polygon poly = (Polygon) geom;
      for (Ring ring : poly.getRings()) {
        for (Position position : ring.getPositions()) {
          latLngs.add(new LatLng(position.getLatitude(), position.getLongitude()));
        }
      }
      this.featureBuilder.initMarkers(this.featureBuilder.createPolygon(), latLngs);
    }
    else if (geom instanceof MultiLineString) {
      MultiLineString multiLine = (MultiLineString) geom;
      for (LineString ls : multiLine.getLineStrings()) {
        latLngs.addAll(this.drawGeometry(ls, new ArrayList<LatLng>()));
      }
    }
    else if (geom instanceof LineString) {
      LineString line = (LineString) geom;
      for (Position position : line.getPositions()) {
        latLngs.add(new LatLng(position.getLatitude(), position.getLongitude()));
      }
      this.featureBuilder.initMarkers(this.featureBuilder.createLineString(), latLngs);
    }
    else if (geom instanceof MultiPoint) {
      MultiPoint multiPoint = (MultiPoint) geom;
      for (Position p : multiPoint.getPositions()) {
        latLngs.add(new LatLng(p.getLatitude(), p.getLongitude()));
      }
      this.featureBuilder.initMarkers(this.featureBuilder.createPoint(), latLngs);
    }
    else if (geom instanceof Point) {
      Point point = (Point) geom;
      Position p = point.getPosition();
      //this.addLatLng(new LatLng(p.getLatitude(), p.getLongitude()));
      latLngs.add(new LatLng(p.getLatitude(), p.getLongitude()));
      this.featureBuilder.initMarkers(this.featureBuilder.createPoint(), latLngs);
    } else {
      // Unsupported geometry type.
      Log.e("Builder", String.format("Unsupported GeoJSON geometry type: %s.", geom.getType()));
    }

    return latLngs;
  }

  public static Bitmap toBitmap(Drawable anyDrawable) {
    Bitmap bmp = Bitmap.createBitmap(anyDrawable.getIntrinsicWidth(), anyDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(bmp);
    anyDrawable.setBounds(0, 0, anyDrawable.getIntrinsicWidth(), anyDrawable.getIntrinsicHeight());
    anyDrawable.draw(canvas);
    return bmp;
  }

  public static Boolean pointsWithinBox(BoundingBox box, ArrayList<LatLng> latLngs) {
    for (LatLng latLng : latLngs) {
      if (!box.contains(latLng)) return false;
    }
    return true;
  }

}