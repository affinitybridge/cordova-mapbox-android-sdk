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

/**
 * Created by tnightingale on 15-04-20.
 */
public class LineBuilder extends Builder {

    final protected int lineColor = Color.RED;

    final protected float strokeWidth = 5;

    protected PathOverlay line;

    public LineBuilder(MapView mapView) {
        super(mapView);
        this.line = new PathOverlay(this.lineColor, this.strokeWidth);
        this.mapView.getOverlays().add(this.line);
    }

    @Override
    protected boolean add(LatLng position) {
        this.line.addPoint(position);

        Log.d("LineBuilder", "add().");

        return true;
    }

    @Override
    protected void remove(int index) {
        this.reset();
        Log.d("LineBuilder", String.format("remove() latLngs.size(): %d, line.getNumberOfPoints(): %d", this.latLngs.size(), this.line.getNumberOfPoints()));
    }

    @Override
    protected void reset() {
        this.line.clearPath();
        this.line.addPoints(this.latLngs);
    }

    @Override
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