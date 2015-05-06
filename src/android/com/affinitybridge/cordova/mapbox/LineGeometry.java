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

    public int size() {
        return this.latLngs.size();
    }

    public void addGhostLatLng(LatLng latLng) {
        this.line.addPoint(latLng);
    }

    public boolean addLatLng(LatLng latLng) {
        Log.d("LineBuilder", String.format("addLatLng(); latLng: (%f, %f)", latLng.getLongitude(), latLng.getLatitude()));
        return this.insertLatLng(-1, latLng);
    }

    public boolean insertLatLng(int position, LatLng latLng) {
        this.line.addPoint(latLng);


        if (position < 0) {
            this.latLngs.add(latLng);
        }
        else {
            Log.d("LineBuilder", String.format("insertLatLng(); position: %d, latLng: (%f, %f)", position, latLng.getLongitude(), latLng.getLatitude()));
            this.latLngs.add(position, latLng);
        }

        return true;
    }

    public void setLatLng(int position, LatLng latLng) {
        this.latLngs.set(position, latLng);
    }

    public int indexOfLatLng(LatLng latLng) {
        return this.latLngs.indexOf(latLng);
    }

    public void remove(LatLng latLng) {
        this.remove(this.indexOfLatLng(latLng));
    }

    public void remove(int position) {
        this.latLngs.remove(position);
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