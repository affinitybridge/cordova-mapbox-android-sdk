package com.affinitybridge.cordova.mapbox;

import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.drawable.Drawable;

import android.util.Log;
import android.view.DragEvent;
import android.view.View;

import com.mapbox.mapboxsdk.api.ILatLng;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.overlay.Icon;
import com.mapbox.mapboxsdk.overlay.Marker;
import com.mapbox.mapboxsdk.views.MapView;
import com.mapbox.mapboxsdk.views.MapViewListener;
import com.mapbox.mapboxsdk.views.util.Projection;

import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by tnightingale on 15-04-21.
 */
abstract class Builder implements MapViewListener {

    protected MapView mapView;

    protected int selected = -1;

    protected String selectedColor = "#FF0000";

    protected String markerColor = "#0000FF";

    protected String markerIcon = "marker-stroked";

    protected Icon icon;

    protected Icon selectedIcon;

    protected ArrayList<LatLng> latLngs;

    protected ArrayList<Marker> markers;

    public Builder(MapView mapView) {
        this.mapView = mapView;
        this.latLngs = new ArrayList<LatLng>();
        this.markers = new ArrayList<Marker>();
        this.selectedIcon = new Icon(this.mapView.getContext(), Icon.Size.LARGE, this.markerIcon, this.selectedColor);
        this.icon = new Icon(this.mapView.getContext(), Icon.Size.SMALL, this.markerIcon, this.markerColor);
    }

    protected void select(int index) {
        Marker marker = this.markers.get(this.selected);
        this.select(index, marker);
    }

    protected void select(int index, Marker marker) {
        this.deselect();
        this.selected = index;
        marker.setIcon(new Icon(this.mapView.getContext(), Icon.Size.LARGE, this.markerIcon, this.selectedColor));
        Log.d("Builder", String.format("select() this.selected: %d", this.selected));
    }

    protected void deselect() {
        if (this.selected < 0) {
            return;
        }
        Marker marker = this.markers.get(this.selected);
        marker.setIcon(new Icon(this.mapView.getContext(), Icon.Size.SMALL, this.markerIcon, this.markerColor));
        this.selected = -1;
    }

    final public void addPoint() {
        LatLng position = mapView.getCenter();
        Marker marker = new Marker(this.mapView, "", "", position);
        marker.setIcon(new Icon(this.mapView.getContext(), Icon.Size.SMALL, this.markerIcon, this.markerColor));

        if (this.add(position)) {
            this.latLngs.add(position);
            this.markers.add(marker);
            this.mapView.addMarker(marker);
        }
        else {
            Log.d("Builder", "Couldn't add point.");
        }
    }

    final public void removePoint() {
        int index = this.selected >= 0 ? this.selected : this.latLngs.size() - 1;
        this.deselect();
        this.removePoint(index);
    }

    final public void removePoint(int index) {
        if (index < 0) {
            return;
        }
        Marker marker = this.markers.get(index);
        this.mapView.removeMarker(marker);
        this.markers.remove(index);
        this.latLngs.remove(index);
        this.remove(index);
    }

    @Override
    public void onShowMarker(MapView mapView, Marker marker) {

    }

    @Override
    public void onHideMarker(MapView mapView, Marker marker) {

    }

    @Override
    public void onTapMarker(MapView mapView, Marker marker) {
        int index = this.markers.indexOf(marker);
        this.select(index, marker);
    }

    @Override
    public void onLongPressMarker(MapView mapView, Marker marker) {
        Log.d("Builder", "onLongPressMarker()");
        int index = this.markers.indexOf(marker);
        this.select(index, marker);
        View.DragShadowBuilder markerShadow = new MarkerShadowBuilder(mapView, marker);
        View.OnDragListener markerDragListener = new MarkerDragEventListener();
        mapView.setOnDragListener(markerDragListener);
        mapView.startDrag(null, markerShadow, null, 0);
    }

    @Override
    public void onTapMap(MapView mapView, ILatLng iLatLng) {}

    @Override
    public void onLongPressMap(MapView mapView, ILatLng iLatLng) {}

    protected void reset() {}

    abstract protected boolean add(LatLng position);

    abstract protected void remove(int index);

    abstract public JSONObject toJSON();

    private class MarkerDragEventListener implements View.OnDragListener {
        private boolean dragStart(View v, DragEvent event) {
            mapView.removeMarker(markers.get(selected));
            return true;
        }

        private boolean dragLocation(View v, DragEvent event) {
            Projection p = mapView.getProjection();
            LatLng latLng = (LatLng) p.fromPixels(event.getX(), event.getY());
            latLngs.set(selected, latLng);

            // Let implementing classes perform reset action.
            reset();

            // Invalidating the view causes a redraw.
            v.invalidate();
            return true;
        }

        private boolean dragDrop(View v, DragEvent event) {
            Projection p = mapView.getProjection();
            Marker m = markers.get(selected);
            LatLng latLng = (LatLng) p.fromPixels(event.getX(), event.getY());
            m.setPoint(latLng);
            mapView.addMarker(m);
            return true;
        }

        public boolean onDrag(View v, DragEvent event) {
            final int action = event.getAction();

            switch (action) {
                case DragEvent.ACTION_DRAG_STARTED:
                    return dragStart(v, event);
                case DragEvent.ACTION_DRAG_ENTERED:
                    return true;
                case DragEvent.ACTION_DRAG_LOCATION:
                    return this.dragLocation(v, event);
                case DragEvent.ACTION_DRAG_EXITED:
                    return true;
                case DragEvent.ACTION_DROP:
                    return this.dragDrop(v, event);
                case DragEvent.ACTION_DRAG_ENDED:
                    return true;
                default:
                    Log.e("MarkerDragEventListener","Unknown action type received by OnDragListener.");
                    break;
            }
            return false;
        }
    }
}
