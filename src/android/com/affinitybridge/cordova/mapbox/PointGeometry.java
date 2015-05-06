package com.affinitybridge.cordova.mapbox;

import com.cocoahero.android.geojson.Feature;
import com.cocoahero.android.geojson.Point;
import com.cocoahero.android.geojson.Position;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.views.MapView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by tnightingale on 15-04-20.
 */
public class PointGeometry implements Builder.GeometryInterface {

    protected MapView mapView;

    protected Builder builder;

    protected int maxMarkers = 1;

    protected int count = 0;

    protected ArrayList<LatLng> latLngs;

    public PointGeometry(MapView mv, Builder builder) {
        this.mapView = mv;
        this.builder = builder;
        this.latLngs = new ArrayList<LatLng>();
    }

    public int size() {
        return this.latLngs.size();
    }

    public void addGhostLatLng(LatLng latLng) {

    }

    public boolean addLatLng(LatLng latLng) {
        return insertLatLng(-1, latLng);
    }

    public boolean insertLatLng(int position, LatLng latLng) {
        if (this.count++ < this.maxMarkers) {
            if (position < 0) {
                this.latLngs.add(latLng);
            }
            else {
                this.latLngs.add(position, latLng);
            }
            return true;
        }
        return false;
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
        this.count--;
    }

    public void reset() {
        this.count = 0;
    }

    public JSONObject toJSON() {
        Point p = new Point();
        for (int i = 0; i < this.latLngs.size(); i++) {
            LatLng latLng = this.latLngs.get(i);
            p.setPosition(new Position(latLng.getLatitude(), latLng.getLongitude()));
        }
        try {
            Feature f = new Feature(p);
            f.setProperties(new JSONObject());
            return f.toJSON();
        } catch (JSONException e) {
            return null;
        }
    }
}