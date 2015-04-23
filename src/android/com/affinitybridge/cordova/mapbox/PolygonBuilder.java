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

/**
 * Created by tnightingale on 15-04-20.
 */
public class PolygonBuilder extends Builder {

    final protected int lineColor = Color.RED;

    final protected float strokeWidth = 5;

    protected PathOverlay outerRing;

    public PolygonBuilder(MapView mapView) {
        super(mapView);
        this.outerRing = new PathOverlay(this.lineColor, this.strokeWidth);
        this.outerRing.getPaint().setStyle(Paint.Style.FILL_AND_STROKE);

        // Prevent glitching when panning & zooming map (see: https://github.com/mapbox/mapbox-android-sdk/issues/461).
        this.outerRing.setOptimizePath(false);

        this.mapView.getOverlays().add(this.outerRing);
    }

    @Override
    protected boolean add(LatLng position) {
        this.outerRing.addPoint(position);

        Log.d("PolygonBuilder", "add().");

        return true;
    }

    @Override
    protected void remove(int index) {
        this.reset();
        Log.d("PolygonBuilder", String.format("remove() latLngs.size(): %d, ring.getNumberOfPoints(): %d", this.latLngs.size(), this.outerRing.getNumberOfPoints()));
    }

    @Override
    protected void reset() {
        this.outerRing.clearPath();
        this.outerRing.addPoints(this.latLngs);
    }

    @Override
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