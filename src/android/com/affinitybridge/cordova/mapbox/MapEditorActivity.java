package com.affinitybridge.cordova.mapbox;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.MenuInflater;
import android.view.Menu;
import android.view.MenuItem;

import android.util.Log;

import com.affinitybridge.mdc.R;

import com.mapbox.mapboxsdk.overlay.GpsLocationProvider;
import com.mapbox.mapboxsdk.overlay.UserLocationOverlay;
import com.mapbox.mapboxsdk.tileprovider.MapTileLayerBase;
import com.mapbox.mapboxsdk.views.MapView;
import com.mapbox.mapboxsdk.geometry.LatLng;

public class MapEditorActivity extends Activity {

    protected MapView mapView;

    protected Builder featureBuilder;

    protected LatLng[] points;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map_editor);

        this.mapView = (MapView) this.findViewById(R.id.mapeditor);

        MapTileLayerBase base = this.mapView.getTileProvider();

        this.mapView.setMinZoomLevel(base.getMinimumZoomLevel());
        this.mapView.setMaxZoomLevel(base.getMaximumZoomLevel());
        this.mapView.setCenter(base.getCenterCoordinate());
        this.mapView.setZoom(0);

        //this.featureBuilder = new PolygonBuilder(this.mapView);
        this.featureBuilder = new LineBuilder(this.mapView);
        //this.featureBuilder = new MarkerBuilder(this.mapView);
        this.mapView.setMapViewListener(this.featureBuilder);
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.context_menu_add_feature, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d("MapEditorActivity", "Menu item selected.");

        // Handle presses on the action bar items.
        switch (item.getItemId()) {
            case R.id.action_add_point:
                Log.d("MapEditorActivity", "Adding point...");
                this.featureBuilder.addPoint();
                return true;

            case R.id.action_remove_point:
                Log.d("MapEditorActivity", "Removing point...");
                this.featureBuilder.removePoint();
                return true;

            case R.id.action_done:
                Log.d("MapEditorActivity", "Done...");
                return true;

            case R.id.action_location_find:
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

            default:
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

}