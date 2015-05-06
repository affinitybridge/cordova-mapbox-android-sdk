package com.affinitybridge.cordova.mapbox;

import android.graphics.Point;
import android.graphics.PointF;

import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.DragEvent;
import android.view.View;

import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.overlay.Icon;
import com.mapbox.mapboxsdk.overlay.Marker;
import com.mapbox.mapboxsdk.overlay.Overlay;
import com.mapbox.mapboxsdk.overlay.SafeDrawOverlay;
import com.mapbox.mapboxsdk.views.MapView;
import com.mapbox.mapboxsdk.views.safecanvas.ISafeCanvas;
import com.mapbox.mapboxsdk.views.util.Projection;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineSegment;

import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by tnightingale on 15-04-21.
 */
class Builder {

    protected MapView mapView;

    protected GeometryInterface activeShape;

    protected int selected = -1;

    protected Vertex lastAdded;

    protected Overlay nextMarkerOverlay;

    protected ArrayList<Vertex> vertices;

    protected ArrayList<Marker> markers;

    protected DraggableItemizedIconOverlay markerOverlay;

    Drawable vertexImage;

    Drawable vertexMiddleImage;

    Drawable vertexSelectedImage;

    public Builder(MapView mv) {
        this.mapView = mv;
        this.vertices = new ArrayList<Vertex>();
        this.markers = new ArrayList<Marker>();

        this.nextMarkerOverlay = new SafeDrawOverlay() {
            /**
             * Most of this is lifted from com.mapbox.mapboxsdk.overlay.ItemizedOverlay's
             * onDrawItem() method.
             *
             * @param iSafeCanvas
             * @param mapView
             * @param b
             */
            @Override
            protected void drawSafe(ISafeCanvas iSafeCanvas, MapView mapView, boolean b) {
                iSafeCanvas.save();

                Projection p = mapView.getProjection();
                int centerX = p.getCenterX(), centerY = p.getCenterY();

                // Calculating marker center to use for offset.
                PointF anchor = new PointF(0.5f, 0.5f);
                int markerWidth = vertexMiddleImage.getIntrinsicWidth(), markerHeight = vertexMiddleImage.getIntrinsicHeight();
                Point offset = new Point((int) (-anchor.x * markerWidth), (int) (-anchor.y * markerHeight));

                // Handling canvas scale to ensure marker is drawn at fixed size.
                final float mapScale = 1 / mapView.getScale();
                iSafeCanvas.scale(mapScale, mapScale, centerX, centerY);

                // Drawable's don't have bounding dimensions by default.
                vertexMiddleImage.setBounds(0, 0, markerWidth, markerHeight);
                Overlay.drawAt(iSafeCanvas.getSafeCanvas(), vertexMiddleImage, new Point(centerX, centerY), offset, false, 0);

                iSafeCanvas.restore();

                activeShape.reset();
                activeShape.addGhostLatLng(mapView.getCenter());
            }
        };

        this.markerOverlay = new DraggableItemizedIconOverlay(this.mapView.getContext(), new ArrayList<Marker>(), new DraggableItemizedIconOverlay.OnItemDraggableGestureListener<Marker>() {
            public boolean onItemSingleTapUp(final int iconOverlayMarkerIndex, final Marker item) {
                int index = markers.indexOf(item);
                Log.d("Builder", String.format("onSingleTapUp() index: %d, selected: %d.", markers.indexOf(item), selected));

                if (index == selected) {
                    return false;
                }

                Vertex vertex = vertices.get(index);

                select(index, vertex);
                return true;
            }

            public boolean onItemLongPress(final int iconOverlayMarkerIndex, final Marker item) {
                int index = markers.indexOf(item);
                Log.d("Builder", String.format("onItemLongPress() index: %d, selected: %d.", index, selected));

                if (vertices.get(index).isGhost()) {
                    // If middle marker; ignore.
                    return false;
                }

                // If real marker; remove it.
                removePoint(index);
                return true;
            }

            public boolean onItemDown(final int iconOverlayMarkerIndex, final Marker item) {
                Log.d("Builder", String.format("onItemDown() index: %d, selected: %d.", markers.indexOf(item), selected));

                if (markers.indexOf(item) != selected) {
                    return false;
                }

                mapView.startDrag(null, new MarkerShadowBuilder(mapView, item), null, 0);
                return true;
            }
        });

        mapView.setOnDragListener(new MarkerDragEventListener());
        this.mapView.addItemizedOverlay(this.markerOverlay);
    }


    public void setVertexImage(Drawable img) {
        this.vertexImage = img;
    }

    public void setVertexSelectedImage(Drawable img) {
        this.vertexSelectedImage = img;
    }

    public void setVertexMiddleImage(Drawable img) {
        this.vertexMiddleImage = img;
    }

    public GeometryInterface getActiveShape() {
        return this.activeShape;
    }

    public GeometryInterface createPoint() {
        GeometryInterface geometry = new PointGeometry(this.mapView, this);
        return geometry;
    }

    public GeometryInterface createLineString() {
        GeometryInterface geometry = new LineGeometry(this.mapView, this);
        return geometry;
    }

    public GeometryInterface createPolygon() {
        GeometryInterface geometry = new PolygonGeometry(this.mapView, this);
        return geometry;
    }

    public void startFeature(GeometryInterface geometry) {
        mapView.addOverlay(this.nextMarkerOverlay);
        this.activeShape = geometry;
    }

    public void stopFeature() {
        mapView.removeOverlay(this.nextMarkerOverlay);
        this.activeShape.reset();
        this.activeShape = null;
    }

    protected void select(int index) {
        Vertex vertex = this.vertices.get(index);
        this.select(index, vertex);
    }

    protected void select(int index, Vertex vertex) {
        this.deselect();
        this.selected = index;

        GeometryInterface geometry = vertex.getOwner();

        Log.d("Builder", String.format("select() vertex.isGhost() ? %b.", vertex.isGhost()));
        // Promote middle vertex to real vertex.

        if (vertex.isGhost()) { // && geometry.insertLatLng(insertPos, vertex.getPoint())) {
            int insertPos = geometry.indexOfLatLng(vertex.getNext().getPoint());
            geometry.insertLatLng(insertPos, vertex.getPoint());
            vertex.setGhost(false);

            updatePrevNext(vertex.getPrev(), vertex);
            updatePrevNext(vertex, vertex.getNext());

            createMiddleMarker(vertex.getPrev(), vertex);
            createMiddleMarker(vertex, vertex.getNext());

            geometry.reset();
        }

        if (this.vertexSelectedImage != null) {
            vertex.getMarker().setHotspot(Marker.HotspotPlace.CENTER);
            vertex.getMarker().setMarker(this.vertexSelectedImage);
        }
        else {
            vertex.getMarker().setIcon(new Icon(this.mapView.getContext(), Icon.Size.SMALL, "", "FF0000"));
        }

        Log.d("Builder", String.format("select() this.selected: %d", this.selected));
    }

    protected void deselect() {
        if (this.selected < 0) {
            return;
        }
        Vertex vertex = this.vertices.get(this.selected);

        if (this.vertexImage != null) {
            vertex.getMarker().setHotspot(Marker.HotspotPlace.CENTER);
            vertex.getMarker().setMarker(this.vertexImage);
        }
        else {
            vertex.getMarker().setIcon(new Icon(this.mapView.getContext(), Icon.Size.SMALL, "", "0000FF"));
        }

        this.selected = -1;
    }

    protected void initMarkers(GeometryInterface geometry, ArrayList<LatLng> latLngs) {
        ArrayList<Vertex> newVertices = new ArrayList<Vertex>();

        // Initialize markers for all vertices.
        for (LatLng latLng : latLngs) {
            Log.d("Builder", String.format("LatLng: (%f, %f).", latLng.getLatitude(), latLng.getLongitude()));
            if (geometry.addLatLng(latLng)) {
                Marker marker = this.createMarker(latLng, this.vertexImage);
                Vertex vertex = new Vertex(geometry, marker);
                newVertices.add(vertex);
            }
        }

        // Add all new vertices to main collection.
        this.vertices.addAll(newVertices);

        // Initialize middle markers.
        Vertex left, right;
        int length = latLngs.size();
        for (int i = 0, j = length - 1; i < length; j = i++) {
            left = newVertices.get(j);
            right = newVertices.get(i);
            this.createMiddleMarker(left, right);
            this.updatePrevNext(left, right);
        }
    }

    protected void createMiddleMarker(Vertex left, Vertex right) {
        if (left == null || right == null) {
            return;
        }

        LatLng middle = this.getMiddleLatLng(left.getPoint(), right.getPoint());
        Marker marker = this.createMarker(middle, this.vertexMiddleImage);

        Vertex vertex = new Vertex(left.getOwner(), marker);
        vertex.setGhost(true);

        this.vertices.add(vertex);

        left.setMiddleRight(vertex);
        right.setMiddleLeft(vertex);
    }

    protected void updatePrevNext(Vertex left, Vertex right) {
        if (left != null) {
            left.setNext(right);
        }
        if (right != null) {
            right.setPrev(left);
        }
    }

    protected LatLng getMiddleLatLng(LatLng left, LatLng right) {
        LineSegment seg = new LineSegment(left.getLongitude(), left.getLatitude(), right.getLongitude(), right.getLatitude());
        Coordinate mid = seg.midPoint();
        return new LatLng(mid.y, mid.x);
    }

    public void addLatLng() {
        if (this.activeShape != null) {
            this.addLatLng(this.activeShape, mapView.getCenter());
        }
    }

    final public void addLatLng(GeometryInterface geometry, LatLng position) {
        if (geometry.addLatLng(position)) {
            Marker marker = this.createMarker(position, this.vertexImage);
            Vertex vertex = new Vertex(geometry, marker);
            this.vertices.add(vertex);

            if (geometry.size() > 1 && this.lastAdded != null) {
                this.createMiddleMarker(this.lastAdded, vertex);
                this.updatePrevNext(this.lastAdded, vertex);
            }

            this.lastAdded = vertex;
        }
    }

    final public Marker createMarker(LatLng latLng, Drawable image) {
        Marker marker = new Marker("", "", latLng);

        if (image != null) {
            marker.setHotspot(Marker.HotspotPlace.CENTER);
            marker.setAnchor(new PointF(0.5f, 0.5f));
            marker.setMarker(image);
        }
        else {
            marker.setIcon(new Icon(this.mapView.getContext(), Icon.Size.SMALL, "", "0000FF"));
        }

        this.markers.add(marker);

        this.markerOverlay.addItem(marker);
        marker.addTo(mapView);

        this.mapView.invalidate();

        return marker;
    }

    final public void removePoint() {
        int index = this.selected >= 0 ? this.selected : - 1;

        this.removePoint(index);
    }

    final public void removePoint(int index) {
        if (index < 0) {
            return;
        }

        // Calling this.deselect() is necessary as removing the vertex and marker will shift
        // subsequent indices in this.vertices & this.markers.
        this.deselect();

        Vertex vertex = this.vertices.remove(index);
        Marker marker = this.markers.remove(index);

        updatePrevNext(vertex.getPrev(), vertex.getNext());
        createMiddleMarker(vertex.getPrev(), vertex.getNext());

        Vertex middleLeft = vertex.getMiddleLeft();
        Vertex middleRight = vertex.getMiddleRight();

        if (middleLeft != null) {
            this.markerOverlay.removeItem(middleLeft.getMarker());
        }
        if (middleRight != null) {
            this.markerOverlay.removeItem(middleRight.getMarker());
        }

        this.markerOverlay.removeItem(marker);

        GeometryInterface geometry = vertex.getOwner();
        geometry.remove(marker.getPoint());

        if (this.lastAdded == vertex) {
            this.lastAdded = vertex.getPrev();
        }

        marker.getDrawable().invalidateSelf();
        mapView.invalidate();
    }

    public JSONObject toJSON() {
        return new JSONObject();//this.activeShape.toJSON();
    }

    private class MarkerDragEventListener implements View.OnDragListener {
        protected Vertex activeVertex;
        protected int activeIndex;

        private boolean dragStart(View v, DragEvent event) {
            Vertex vertex = vertices.get(selected);

            GeometryInterface geometry = vertex.getOwner();
            this.activeVertex = vertex;
            this.activeIndex = geometry.indexOfLatLng(vertex.getPoint());

            markerOverlay.removeItem(vertex.getMarker());
            Log.d("Builder", String.format("dragStart() selected: %d, activeIndex: %d, size: %d", selected, activeIndex, geometry.size()));

            return true;
        }

        private boolean dragLocation(View v, DragEvent event) {
            Projection p = mapView.getProjection();
            LatLng latLng = (LatLng) p.fromPixels(event.getX(), event.getY());

            this.activeVertex.getOwner().setLatLng(this.activeIndex, latLng);
            // Let implementing classes perform reset action.
            this.activeVertex.getOwner().reset();

            Vertex vertex = vertices.get(selected);
            Vertex prev = vertex.getPrev();
            Vertex next = vertex.getNext();

            if (prev != null) {
                vertex.getMiddleLeft().setPoint(getMiddleLatLng(prev.getPoint(), latLng));
            }

            if (next != null) {
                vertex.getMiddleRight().setPoint(getMiddleLatLng(latLng, next.getPoint()));
            }

            // Invalidating the view causes a redraw.
            v.invalidate();
            return true;
        }

        private boolean dragDrop(View view, DragEvent event) {
            Projection p = mapView.getProjection();
            Vertex v = vertices.get(selected);

            LatLng latLng = (LatLng) p.fromPixels(event.getX(), event.getY());
            v.getOwner().setLatLng(this.activeIndex, latLng);

            v.setPoint(latLng);
            markerOverlay.addItem(v.getMarker());
            v.getMarker().addTo(mapView);

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

    public static interface GeometryInterface {

        public void reset();

        public boolean addLatLng(LatLng latLng);

        public void addGhostLatLng(LatLng latLng);

        public boolean insertLatLng(int position, LatLng latLng);

        public void setLatLng(int position, LatLng latLng);

        public int indexOfLatLng(LatLng latLng);

        public void remove(int position);

        public void remove(LatLng latLng);

        public JSONObject toJSON();

        public int size();

    }
}