package com.affinitybridge.cordova.mapbox;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PointF;

import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.util.Log;
import android.view.DragEvent;
import android.view.View;

import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.overlay.Icon;
import com.mapbox.mapboxsdk.overlay.Marker;
import com.mapbox.mapboxsdk.views.MapView;
import com.mapbox.mapboxsdk.views.util.Projection;

import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by tnightingale on 15-04-21.
 */
abstract class Builder {

    protected MapView mapView;

    protected int selected = -1;

    protected ArrayList<LatLng> latLngs;

    protected ArrayList<Marker> markers;

    protected DraggableItemizedIconOverlay markerOverlay;

    Drawable vertexImage;

    Drawable vertexSelectedImage;

    public Builder(MapView mv) {
        this.mapView = mv;
        this.latLngs = new ArrayList<LatLng>();
        this.markers = new ArrayList<Marker>();

        this.markerOverlay = new DraggableItemizedIconOverlay(this.mapView.getContext(), new ArrayList<Marker>(), new DraggableItemizedIconOverlay.OnItemDraggableGestureListener<Marker>() {
            public boolean onItemSingleTapUp(final int index, final Marker item) {
                return false;
            }

            public boolean onItemLongPress(final int index, final Marker item) {
                return false;
            }

            public boolean onItemDown(final int index, final Marker item) {
                select(markers.indexOf(item), item);

                mapView.setOnDragListener(new MarkerDragEventListener());
                mapView.startDrag(null, new MarkerShadowBuilder(mapView, item), null, 0);

                return true;
            }
        });

        this.mapView.addItemizedOverlay(this.markerOverlay);
    }


    public void setVertexImage(Drawable img) {
        this.vertexImage = img;
    }

    public void setVertexSelectedImage(Drawable img) {
        this.vertexSelectedImage = img;
    }

    protected void select(int index) {
        Marker marker = this.markers.get(this.selected);
        this.select(index, marker);
    }

    protected void select(int index, Marker marker) {
        this.deselect();
        this.selected = index;

        if (this.vertexSelectedImage != null) {
            marker.setHotspot(Marker.HotspotPlace.CENTER);
            marker.setMarker(this.vertexSelectedImage);
        }
        else {
            marker.setIcon(new Icon(this.mapView.getContext(), Icon.Size.SMALL, "", "FF0000"));
        }

        Log.d("Builder", String.format("select() this.selected: %d", this.selected));
    }

    protected void deselect() {
        if (this.selected < 0) {
            return;
        }
        Marker marker = this.markers.get(this.selected);

        if (this.vertexImage != null) {
            marker.setHotspot(Marker.HotspotPlace.CENTER);
            marker.setMarker(this.vertexImage);
        }
        else {
            marker.setIcon(new Icon(this.mapView.getContext(), Icon.Size.SMALL, "", "0000FF"));
        }

        this.selected = -1;
    }

    final public void addPoint() {
        LatLng position = mapView.getCenter();
        Marker marker = new Marker("", "", position);
        marker.setAnchor(new PointF(0.5f, 0.5f));

        if (this.vertexImage != null) {
            marker.setHotspot(Marker.HotspotPlace.CENTER);
            marker.setMarker(this.vertexImage);
        }
        else {
            marker.setIcon(new Icon(this.mapView.getContext(), Icon.Size.SMALL, "", "0000FF"));
        }

        if (this.add(position)) {
            this.latLngs.add(position);
            this.markers.add(marker);
            this.markerOverlay.addItem(marker);
            marker.addTo(mapView);
            this.mapView.invalidate();
            Log.d("Builder", String.format("Added point, this.latLngs.size(): %d", this.latLngs.size()));
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
        this.markerOverlay.removeItem(marker);
        this.markers.remove(index);
        this.latLngs.remove(index);
        this.remove(index);
        mapView.invalidate();
    }

    protected void reset() {}

    abstract protected boolean add(LatLng position);

    abstract protected void remove(int index);

    abstract public JSONObject toJSON();

    private class MarkerDragEventListener implements View.OnDragListener {
        private boolean dragStart(View v, DragEvent event) {
            markerOverlay.removeItem(markers.get(selected));
            Log.d("MarkerDragEventL", String.format("dragStart(), latLngs.size(): %d", latLngs.size()));
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
            markerOverlay.addItem(m);
            m.addTo(mapView);
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
