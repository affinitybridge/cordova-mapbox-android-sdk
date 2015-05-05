package com.affinitybridge.cordova.mapbox;

import android.graphics.Color;
import android.graphics.Paint;

import android.util.Log;

import com.cocoahero.android.geojson.Feature;
import com.cocoahero.android.geojson.Polygon;
import com.cocoahero.android.geojson.Position;
import com.cocoahero.android.geojson.Ring;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.overlay.PathOverlay;
import com.mapbox.mapboxsdk.views.MapView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by tnightingale on 15-04-20.
 */
public class PolygonGeometry implements Builder.GeometryInterface {

    final protected int lineColor = Color.RED;

    final protected float strokeWidth = 5;

    protected MapView mapView;

    protected Builder builder;

    protected PathOverlay outerRingStroke;

    protected PathOverlay outerRingFill;

    protected ArrayList<LatLng> latLngs;

    public PolygonGeometry(MapView mv, Builder builder) {
        this.mapView = mv;
        this.builder = builder;
        this.latLngs = new ArrayList<LatLng>();
        this.outerRingStroke = new PathOverlay(this.lineColor, this.strokeWidth);
        this.outerRingStroke.getPaint().setStyle(Paint.Style.STROKE);
        this.outerRingStroke.getPaint().setAlpha(100);
        this.outerRingStroke.getPaint().setStrokeWidth(3);

        this.outerRingFill = new PathOverlay(this.lineColor, 0);
        this.outerRingFill.getPaint().setStyle(Paint.Style.FILL);
        this.outerRingFill.getPaint().setAlpha(50);

        // Prevent glitching when panning & zooming map (see: https://github.com/mapbox/mapbox-android-sdk/issues/461).
        this.outerRingStroke.setOptimizePath(false);
        this.outerRingFill.setOptimizePath(false);

        this.mapView.getOverlays().add(this.outerRingStroke);
        this.mapView.getOverlays().add(this.outerRingFill);
    }

    public ArrayList<LatLng> getLatLngs() {
        return this.latLngs;
    }

    public boolean add(LatLng position) {
        this.outerRingStroke.addPoint(position);
        this.outerRingFill.addPoint(position);

        Log.d("PolygonBuilder", "add().");

        return true;
    }

    public void remove(int index) {
        this.reset();
        Log.d("PolygonBuilder", String.format("remove() latLngs.size(): %d, ring.getNumberOfPoints(): %d", this.latLngs.size(), this.outerRingStroke.getNumberOfPoints()));
    }

    public void reset() {
        this.outerRingStroke.clearPath();
        this.outerRingFill.clearPath();
        this.outerRingStroke.addPoints(this.latLngs);
        this.outerRingFill.addPoints(this.latLngs);
    }

    public JSONObject toJSON() {
        Ring ring = new Ring();

        for (int i = 0; i < this.latLngs.size(); i++) {
            LatLng latLng = this.latLngs.get(i);
            ring.addPosition(new Position(latLng.getLatitude(), latLng.getLongitude()));
        }

        Polygon p = new Polygon(ring);
        try {
            Feature f = new Feature(p);
            f.setProperties(new JSONObject());
            return f.toJSON();
        } catch (JSONException e) {
            return null;
        }
    }

}