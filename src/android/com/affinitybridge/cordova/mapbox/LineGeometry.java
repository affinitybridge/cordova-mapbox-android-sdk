package com.affinitybridge.cordova.mapbox;

import android.graphics.Color;
import android.util.Log;

import com.cocoahero.android.geojson.Feature;
import com.cocoahero.android.geojson.LineString;
import com.cocoahero.android.geojson.Position;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.overlay.PathOverlay;
import com.mapbox.mapboxsdk.views.MapView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by tnightingale on 15-04-20.
 */
public class LineGeometry implements Builder.GeometryInterface {

    final protected int lineColor = Color.RED;

    final protected float strokeWidth = 5;

    protected MapView mapView;

    protected Builder builder;

    protected PathOverlay line;

    protected ArrayList<LatLng> latLngs;

    public LineGeometry(MapView mv, Builder builder) {
        this.mapView = mv;
        this.builder = builder;
        this.latLngs = new ArrayList<LatLng>();
        this.line = new PathOverlay(this.lineColor, this.strokeWidth);
        this.mapView.getOverlays().add(this.line);
    }

    public ArrayList<LatLng> getLatLngs() {
        return this.latLngs;
    }

    public boolean add(LatLng position) {
        this.line.addPoint(position);

        Log.d("LineBuilder", "add().");

        return true;
    }

    public void remove(int index) {
        this.reset();
        Log.d("LineBuilder", String.format("remove() latLngs.size(): %d, line.getNumberOfPoints(): %d", this.latLngs.size(), this.line.getNumberOfPoints()));
    }

    public void reset() {
        this.line.clearPath();
        this.line.addPoints(this.latLngs);
    }

    public JSONObject toJSON() {
        LineString ls = new LineString();
        for (int i = 0; i < this.latLngs.size(); i++) {
            LatLng latLng = this.latLngs.get(i);
            ls.addPosition(new Position(latLng.getLatitude(), latLng.getLongitude()));
        }
        try {
            Feature f = new Feature(ls);
            f.setProperties(new JSONObject());
            return f.toJSON();
        } catch (JSONException e) {
            return null;
        }
    }

}