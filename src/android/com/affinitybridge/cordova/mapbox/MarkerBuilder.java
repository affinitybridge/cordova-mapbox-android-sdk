package com.affinitybridge.cordova.mapbox;

import android.graphics.Color;
import android.util.Log;

import com.cocoahero.android.geojson.Feature;
import com.cocoahero.android.geojson.Point;
import com.cocoahero.android.geojson.Position;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.views.MapView;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by tnightingale on 15-04-20.
 */
public class MarkerBuilder extends Builder {

    public MarkerBuilder(MapView mapView) {
        super(mapView);
    }

    @Override
    protected boolean add(LatLng position) {
        return this.latLngs.isEmpty();
    }

    @Override
    protected void remove(int index) {

    }

    @Override
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