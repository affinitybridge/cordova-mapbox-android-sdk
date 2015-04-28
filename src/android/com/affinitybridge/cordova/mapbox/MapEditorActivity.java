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
import android.view.MenuInflater;
import android.view.Menu;
import android.view.MenuItem;

import android.util.Log;

import com.mapbox.mapboxsdk.overlay.GpsLocationProvider;
import com.mapbox.mapboxsdk.overlay.UserLocationOverlay;
import com.mapbox.mapboxsdk.tileprovider.MapTileLayerBase;
import com.mapbox.mapboxsdk.views.MapView;

public class MapEditorActivity extends Activity {

    protected MapView mapView;

    protected Builder featureBuilder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(this.resource("layout", "map_editor"));

        this.mapView = (MapView) this.findViewById(this.resource("id", "mapeditor"));

        MapTileLayerBase base = this.mapView.getTileProvider();

        this.mapView.setMinZoomLevel(base.getMinimumZoomLevel());
        this.mapView.setMaxZoomLevel(base.getMaximumZoomLevel());
        this.mapView.setCenter(base.getCenterCoordinate());
        this.mapView.setZoom(0);

        Resources res = this.mapView.getResources();
        GradientDrawable vertexImg = (GradientDrawable) res.getDrawable(this.resource("drawable", "vertex_marker"));

        vertexImg.setColor(Color.parseColor("#FFFFFF"));
        Drawable vertexImage = new BitmapDrawable(res, MapEditorActivity.toBitmap(vertexImg));
        vertexImg.setColor(Color.parseColor("#FFFF00"));
        Drawable vertexSelectedImage = new BitmapDrawable(res, MapEditorActivity.toBitmap(vertexImg));

        this.featureBuilder = new PolygonBuilder(this.mapView);
        //this.featureBuilder = new LineBuilder(this.mapView);
        //this.featureBuilder = new MarkerBuilder(this.mapView);

        this.featureBuilder.setVertexImage(vertexImage);
        this.featureBuilder.setVertexSelectedImage(vertexSelectedImage);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = this.getMenuInflater();
        inflater.inflate(this.resource("menu", "context_menu_add_feature"), menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d("MapEditorActivity", "Menu item selected.");

        // Handle presses on the action bar items.
        int id = item.getItemId();
        if (id == this.resource("id", "action_add_point")) {
            Log.d("MapEditorActivity", "Adding point...");
            this.featureBuilder.addPoint();
            return true;
        }
        else if (id == this.resource("id", "action_remove_point")) {
            Log.d("MapEditorActivity", "Removing point...");
            this.featureBuilder.removePoint();
            return true;
        }
        else if (id == this.resource("id", "action_done")) {
            Log.d("MapEditorActivity", "Done...");
            return true;
        }
        else if (id == this.resource("id", "action_location_find")) {
            Log.d("MapEditorActivity", "Find location action selected.");
            GpsLocationProvider myLocationProvider = new GpsLocationProvider(this);
            UserLocationOverlay myLocationOverlay = new UserLocationOverlay(myLocationProvider, mapView);
            myLocationOverlay.enableMyLocation();
            myLocationOverlay.setDrawAccuracyEnabled(true);
            this.mapView.setUserLocationEnabled(true);
            this.mapView.setUserLocationTrackingMode(UserLocationOverlay.TrackingMode.FOLLOW);
            this.mapView.setUserLocationRequiredZoom(12);
            this.mapView.getOverlays().add(myLocationOverlay);
            return true;
        }
        else {
            return super.onOptionsItemSelected(item);
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

    public static Bitmap toBitmap(Drawable anyDrawable){
        Bitmap bmp = Bitmap.createBitmap(anyDrawable.getIntrinsicWidth(), anyDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        anyDrawable.setBounds(0, 0, anyDrawable.getIntrinsicWidth(), anyDrawable.getIntrinsicHeight());
        anyDrawable.draw(canvas);
        return bmp;
    }

}