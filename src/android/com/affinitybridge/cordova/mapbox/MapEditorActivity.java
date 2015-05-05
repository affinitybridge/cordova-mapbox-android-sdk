package com.affinitybridge.cordova.mapbox;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.ActionMode;
import android.view.MenuInflater;
import android.view.Menu;
import android.view.MenuItem;

import android.util.Log;
import android.view.View;

import com.cocoahero.android.geojson.Feature;
import com.cocoahero.android.geojson.FeatureCollection;
import com.cocoahero.android.geojson.GeoJSON;
import com.cocoahero.android.geojson.GeoJSONObject;
import com.cocoahero.android.geojson.Geometry;
import com.cocoahero.android.geojson.LineString;
import com.cocoahero.android.geojson.Point;
import com.cocoahero.android.geojson.Polygon;
import com.cocoahero.android.geojson.Position;
import com.cocoahero.android.geojson.Ring;
import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;
import com.mapbox.mapboxsdk.geometry.BoundingBox;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.overlay.GpsLocationProvider;
import com.mapbox.mapboxsdk.overlay.UserLocationOverlay;
import com.mapbox.mapboxsdk.tileprovider.MapTileLayerBase;
import com.mapbox.mapboxsdk.util.GeoUtils;
import com.mapbox.mapboxsdk.views.MapView;

import org.chromium.content.browser.SelectActionModeCallback;
import org.json.JSONException;

import java.util.ArrayList;

public class MapEditorActivity extends Activity {

    protected MapView mapView;

    protected Builder featureBuilder;

    protected ActionMode mActionMode;

    protected boolean isCreatingFeature = false;

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
            }
            else {
                return false;
            }
        }

        // Called when the user exits the action mode
        @Override
        public void onDestroyActionMode(ActionMode mode) {
            Log.d("ActionMode.Callback", "onDestroyActionMode()");
            mActionMode = null;
            findViewById(resource("id", "action_add_vertex")).setVisibility(View.INVISIBLE);
            findViewById(resource("id", "actions_add")).setVisibility(View.VISIBLE);
            isCreatingFeature = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(this.resource("layout", "map_editor"));

        this.mapView = (MapView) this.findViewById(this.resource("id", "mapeditor"));

        this.initAddMenuButtons();

        MapTileLayerBase base = this.mapView.getTileProvider();

        this.mapView.setMinZoomLevel(base.getMinimumZoomLevel());
        this.mapView.setMaxZoomLevel(base.getMaximumZoomLevel());
        this.mapView.setCenter(base.getCenterCoordinate());
        this.mapView.setZoom(0);

        Resources res = this.mapView.getResources();
        GradientDrawable vertexImg = (GradientDrawable) res.getDrawable(this.resource("drawable", "vertex_marker"));

        // Pre-rendering images for the three states of vertex marker; default, selected and ghost.
        vertexImg.setColor(Color.parseColor("#FFFFFF"));
        Drawable vertexImage = new BitmapDrawable(res, MapEditorActivity.toBitmap(vertexImg));
        vertexImg.setAlpha(60);
        Drawable vertexMiddleImage = new BitmapDrawable(res, MapEditorActivity.toBitmap(vertexImg));
        vertexImg.setAlpha(255);
        vertexImg.setColor(Color.parseColor("#FFFF00"));
        Drawable vertexSelectedImage = new BitmapDrawable(res, MapEditorActivity.toBitmap(vertexImg));

        this.featureBuilder = new Builder(this.mapView);

        this.featureBuilder.setVertexImage(vertexImage);
        this.featureBuilder.setVertexMiddleImage(vertexMiddleImage);
        this.featureBuilder.setVertexSelectedImage(vertexSelectedImage);

        Intent intent = this.getIntent();
        String jsonStr = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (!jsonStr.equals("")) {
            Log.d("MapEditorActivity", jsonStr);
            try {
                GeoJSONObject geojson = GeoJSON.parse(jsonStr);
                this.parseGeoJSON(geojson);
            } catch (JSONException e) {
                Log.e("MapEditorActivity", e.getMessage());
                e.printStackTrace();
            }
        }
        else {
            Log.d("MapEditorActivity", "No GeoJSON, zooming to user location.");
            this.mapView.setUserLocationEnabled(true);
            this.mapView.setCenter(this.mapView.getUserLocation());
            this.mapView.setZoom(14);
        }
    }

    public void initAddMenuButtons() {
        final FloatingActionsMenu addMenu = (FloatingActionsMenu) this.findViewById(this.resource("id", "actions_add"));
        final FloatingActionButton addVertex = (FloatingActionButton) this.findViewById(this.resource("id", "action_add_vertex"));

        addMenu.setOnFloatingActionsMenuUpdateListener(new FloatingActionsMenu.OnFloatingActionsMenuUpdateListener() {
            @Override
            public void onMenuExpanded() {

            }

            @Override
            public void onMenuCollapsed() {
                if (isCreatingFeature) {
                    addMenu.setVisibility(View.INVISIBLE);
                    addVertex.setVisibility(View.VISIBLE);
                }
            }
        });

        FloatingActionButton addPoint = new FloatingActionButton(this.getBaseContext());
        addPoint.setTitle("Add point");
        addPoint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isCreatingFeature = true;
                addMenu.collapse();
                mActionMode = startActionMode(mActionModeCallback);
                addVertex.setOnClickListener(new MarkerBuilder(mapView, featureBuilder));
            }
        });
        addMenu.addButton(addPoint);

        FloatingActionButton addLine = new FloatingActionButton(this.getBaseContext());
        addLine.setTitle("Add line");
        addLine.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isCreatingFeature = true;
                addMenu.collapse();
                mActionMode = startActionMode(mActionModeCallback);
                addVertex.setOnClickListener(new LineBuilder(mapView, featureBuilder));
            }
        });
        addMenu.addButton(addLine);

        FloatingActionButton addPoly = new FloatingActionButton(this.getBaseContext());
        addPoly.setTitle("Add polygon");
        addPoly.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isCreatingFeature = true;
                addMenu.collapse();
                mActionMode = startActionMode(mActionModeCallback);
                addVertex.setOnClickListener(new PolygonBuilder(mapView, featureBuilder));
            }
        });
        addMenu.addButton(addPoly);
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

    public static Bitmap toBitmap(Drawable anyDrawable){
        Bitmap bmp = Bitmap.createBitmap(anyDrawable.getIntrinsicWidth(), anyDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        anyDrawable.setBounds(0, 0, anyDrawable.getIntrinsicWidth(), anyDrawable.getIntrinsicHeight());
        anyDrawable.draw(canvas);
        return bmp;
    }

    public void parseGeoJSON(GeoJSONObject geojson) {
        Log.d("Builder", "parseGeoJSON()");
        Log.d("Builder", String.format("Type: %s.", geojson.getType()));

        ArrayList<LatLng> coords = new ArrayList<LatLng>();

        if (geojson instanceof FeatureCollection) {
            FeatureCollection fc = (FeatureCollection) geojson;
            for (Feature f : fc.getFeatures()) {
                this.parseGeoJSON(f);
            }
        }
        else if (geojson instanceof Feature) {
            Log.d("Builder", "instanceof Feature");
            Feature feature = (Feature) geojson;
            drawGeometry(feature.getGeometry(), coords);
        }
        else {
            Log.d("Builder", "Not instanceof Feature.");
        }
    }

    public void drawGeometry(Geometry geom, ArrayList<LatLng> coords) {
        Builder.BuilderInterface builder;
//        if (geom instanceof GeometryCollection) {
//            GeometryCollection gc = (GeometryCollection) geom;
//            for (Geometry g : gc.getGeometries()) {
//                this.drawGeometry(g);
//            }
//        }
//        else if (geom instanceof MultiPolygon) {
//            MultiPolygon multiPoly = (MultiPolygon) geom;
//            for (Polygon poly : multiPoly.getPolygons()) {
//                this.drawGeometry(poly);
//            }
//        }
        if (geom instanceof Polygon) {
            builder = new PolygonBuilder(this.mapView, this.featureBuilder);
            Polygon poly = (Polygon) geom;
            for (Ring ring : poly.getRings()) {
                for (Position position : ring.getPositions()) {
                    //this.addLatLng(new LatLng(position.getLatitude(), position.getLongitude()));
                    builder.getLatLngs().add(new LatLng(position.getLatitude(), position.getLongitude()));
                }
            }
            this.featureBuilder.initMarkers(builder);

            BoundingBox box = GeoUtils.findBoundingBoxForGivenLocations(builder.getLatLngs(), 0.1);
            mapView.zoomToBoundingBox(box);
        }
//        else if (geom instanceof MultiLineString) {
//            MultiLineString multiLine = (MultiLineString) geom;
//            for (LineString ls : multiLine.getLineStrings()) {
//                this.drawGeometry(ls);
//            }
//        }
        else if (geom instanceof LineString) {
            builder = new LineBuilder(this.mapView, this.featureBuilder);
            LineString line = (LineString) geom;
            for (Position position : line.getPositions()) {
                //this.addLatLng(new LatLng(position.getLatitude(), position.getLongitude()));
                builder.getLatLngs().add(new LatLng(position.getLatitude(), position.getLongitude()));
            }
            this.featureBuilder.initMarkers(builder);

            BoundingBox box = GeoUtils.findBoundingBoxForGivenLocations(builder.getLatLngs(), 0.1);
            mapView.zoomToBoundingBox(box);
        }
//        else if (geom instanceof MultiPoint) {
//            MultiPoint multiPoint = (MultiPoint) geom;
//            for (Position p : multiPoint.getPositions()) {
//                this.addLatLng(new LatLng(p.getLatitude(), p.getLongitude()));
//            }
//        }
        else if (geom instanceof Point) {
            builder = new MarkerBuilder(this.mapView, this.featureBuilder);
            Point point = (Point) geom;
            Position p = point.getPosition();
            //this.addLatLng(new LatLng(p.getLatitude(), p.getLongitude()));
            builder.getLatLngs().add(new LatLng(p.getLatitude(), p.getLongitude()));
            this.featureBuilder.initMarkers(builder);

            BoundingBox box = GeoUtils.findBoundingBoxForGivenLocations(builder.getLatLngs(), 0.1);
            mapView.zoomToBoundingBox(box);
        }
        else {
            // Unsupported geometry type.
            Log.e("Builder", String.format("Unsupported GeoJSON geometry type: %s.", geom.getType()));
        }
    }

}