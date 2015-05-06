package com.affinitybridge.cordova.mapbox;

import android.content.Context;
import android.view.MotionEvent;

import com.mapbox.mapboxsdk.overlay.ItemizedIconOverlay;
import com.mapbox.mapboxsdk.overlay.ItemizedIconOverlay.OnItemGestureListener;
import com.mapbox.mapboxsdk.overlay.Marker;
import com.mapbox.mapboxsdk.views.MapView;
import com.mapbox.mapboxsdk.views.util.Projection;

import java.util.List;

public class DraggableItemizedIconOverlay extends ItemizedIconOverlay {
    protected OnItemDraggableGestureListener mOnItemGestureListener;

    public DraggableItemizedIconOverlay(Context paramContext, List<Marker> paramList, OnItemDraggableGestureListener<Marker> paramOnItemDraggableGestureListener) {
        this(paramContext, paramList, paramOnItemDraggableGestureListener, false);
    }

    public DraggableItemizedIconOverlay(Context paramContext, List<Marker> paramList, OnItemDraggableGestureListener<Marker> paramOnItemDraggableGestureListener, boolean paramBoolean) {
        super(paramContext, paramList, paramOnItemDraggableGestureListener, paramBoolean);
        this.mOnItemGestureListener = paramOnItemDraggableGestureListener;
    }

    public boolean onTouchEvent(MotionEvent paramMotionEvent, MapView paramMapView) {
        Projection localProjection = paramMapView.getProjection();
        float x = paramMotionEvent.getX();
        float y = paramMotionEvent.getY();
        int i = 0;
        while (i < this.mItemList.size()) {
            if ((markerHitTest(getItem(i), localProjection, x, y)) && (this.mOnItemGestureListener.onItemDown(i, this.mItemList.get(i)))) {
                return true;
            }
            i += 1;
        }
        return super.onTouchEvent(paramMotionEvent, paramMapView);
    }

    public static abstract interface OnItemDraggableGestureListener<T> extends ItemizedIconOverlay.OnItemGestureListener<T> {
        public abstract boolean onItemDown(int paramInt, T paramT);
    }
}