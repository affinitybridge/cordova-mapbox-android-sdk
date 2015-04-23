package com.affinitybridge.cordova.mapbox;

import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.view.View;

import com.mapbox.mapboxsdk.overlay.Marker;

class MarkerShadowBuilder extends View.DragShadowBuilder {
    private Drawable shadow;
    private Marker marker;

    public MarkerShadowBuilder(View v, Marker m) {
        super(v);
        this.marker = m;
        this.shadow = marker.getDrawable();
    }

    @Override
    public void onProvideShadowMetrics(Point size, Point touch) {
        int width = this.marker.getWidth();
        int height = this.marker.getRealHeight();

        this.shadow.setBounds(0, 0, width, height);

        size.set(width, height);
        touch.set(width / 2, height / 2);
    }

    @Override
    public void onDrawShadow(Canvas canvas) {
        this.shadow.draw(canvas);
    }
}