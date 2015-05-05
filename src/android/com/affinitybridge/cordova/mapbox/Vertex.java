package com.affinitybridge.cordova.mapbox;

import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.overlay.Marker;

class Vertex {

    protected boolean ghost = false;

    protected Marker marker;

    protected Vertex prev;

    protected Vertex next;

    protected Vertex middleLeft;

    protected Vertex middleRight;

    protected Builder.BuilderInterface owner;

    public Vertex(Builder.BuilderInterface o, Marker m) {
        this.marker = m;
        this.owner = o;
    }

    public Builder.BuilderInterface getOwner() {
        return this.owner;
    }

    public Marker getMarker() {
        return this.marker;
    }

    public LatLng getPoint() {
        return this.marker.getPoint();
    }

    public void setPoint(LatLng latLng) {
        this.marker.setPoint(latLng);
    }

    public void setMiddleLeft(Vertex vertex) {
        vertex.setNext(this);
        this.middleLeft = vertex;
    }

    public void setMiddleRight(Vertex vertex) {
        vertex.setPrev(this);
        this.middleRight = vertex;
    }

    public void setPrev(Vertex vertex) {
        this.prev = vertex;
    }

    public void setNext(Vertex vertex) {
        this.next = vertex;
    }

    public Vertex getPrev() {
        return this.prev;
    }

    public Vertex getNext() {
        return this.next;
    }

    public Vertex getMiddleLeft() {
        return middleLeft;
    }

    public Vertex getMiddleRight() {
        return middleRight;
    }

        public boolean isGhost() {
        return this.ghost;
    }

    public void setGhost(boolean isGhost) {
        this.ghost = isGhost;
    }

}