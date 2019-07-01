package com.github.react.sextant;

import android.graphics.Rect;
import android.view.View;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.graphics.Canvas;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.GestureDetector;
import android.view.ScaleGestureDetector;
import android.widget.Scroller;


import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.annotations.ReactProp;
import com.facebook.react.uimanager.events.RCTEventEmitter;
import com.facebook.react.common.MapBuilder;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.facebook.common.logging.FLog;
import com.facebook.react.common.ReactConstants;

import static java.lang.String.format;
import java.lang.ClassCastException;

public class MuPdfView extends View implements GestureDetector.OnGestureListener,
        ScaleGestureDetector.OnScaleGestureListener {
    private ThemedReactContext context;

    protected float viewScale, minScale, maxScale;
    protected Bitmap bitmap;
    protected int bitmapW, bitmapH;
    protected int canvasW, canvasH;
    protected int scrollX, scrollY;
    protected Link[] links;
    protected Quad[] hits;
    protected boolean showLinks;

    protected GestureDetector detector;
    protected ScaleGestureDetector scaleDetector;
    protected Scroller scroller;
    protected boolean error;
    protected Paint errorPaint;
    protected Path errorPath;
    protected Paint linkPaint;
    protected Paint hitPaint;

    private static MuPdfView instance = null;

    public MuPdfView(ThemedReactContext ctx, AttributeSet atts) {
        super(ctx, atts);

        this.context = ctx;
        this.instance = this;

        scroller = new Scroller(ctx);
        detector = new GestureDetector(ctx, this);
        scaleDetector = new ScaleGestureDetector(ctx, this);

        viewScale = 1;
        minScale = 1;
        maxScale = 2;

        linkPaint = new Paint();
        linkPaint.setARGB(32, 0, 0, 255);

        hitPaint = new Paint();
        hitPaint.setARGB(32, 255, 0, 0);
        hitPaint.setStyle(Paint.Style.FILL);

        errorPaint = new Paint();
        errorPaint.setARGB(255, 255, 80, 80);
        errorPaint.setStrokeWidth(5);
        errorPaint.setStyle(Paint.Style.STROKE);

        errorPath = new Path();
        errorPath.moveTo(-100, -100);
        errorPath.lineTo(100, 100);
        errorPath.moveTo(100, -100);
        errorPath.lineTo(-100, 100);
    }

//    public void setActionListener(DocumentActivity l) {
//        actionListener = l;
//    }

    public void setError() {
        if (bitmap != null)
            bitmap.recycle();
        error = true;
        links = null;
        hits = null;
        bitmap = null;
        invalidate();
    }

    public void setBitmap(Bitmap b, boolean wentBack, Link[] ls, Quad[] hs) {
        if (bitmap != null)
            bitmap.recycle();
        error = false;
        links = ls;
        hits = hs;
        bitmap = b;
        bitmapW = (int)(bitmap.getWidth() * viewScale);
        bitmapH = (int)(bitmap.getHeight() * viewScale);
        scroller.forceFinished(true);
        scrollX = wentBack ? bitmapW - canvasW : 0;
        scrollY = wentBack ? bitmapH - canvasH : 0;
        invalidate();
    }

    public void resetHits() {
        hits = null;
        invalidate();
    }

    public void onSizeChanged(int w, int h, int ow, int oh) {
        canvasW = w;
        canvasH = h;
//        actionListener.onPageViewSizeChanged(w, h);
    }

    public boolean onTouchEvent(MotionEvent event) {
        detector.onTouchEvent(event);
        scaleDetector.onTouchEvent(event);
        return true;
    }

    public boolean onDown(MotionEvent e) {
        scroller.forceFinished(true);
        return true;
    }

    public void onShowPress(MotionEvent e) { }

    public void onLongPress(MotionEvent e) {
        showLinks = !showLinks;
        invalidate();
    }

    public boolean onSingleTapUp(MotionEvent e) {

        return true;
    }

    public boolean onScroll(MotionEvent e1, MotionEvent e2, float dx, float dy) {
        if (bitmap != null) {
            scrollX += (int)dx;
            scrollY += (int)dy;
            scroller.forceFinished(true);
            invalidate();
        }
        return true;
    }

    public boolean onFling(MotionEvent e1, MotionEvent e2, float dx, float dy) {
        if (bitmap != null) {
            int maxX = bitmapW > canvasW ? bitmapW - canvasW : 0;
            int maxY = bitmapH > canvasH ? bitmapH - canvasH : 0;
            scroller.forceFinished(true);
            scroller.fling(scrollX, scrollY, (int)-dx, (int)-dy, 0, maxX, 0, maxY);
            invalidate();
        }
        return true;
    }

    public boolean onScaleBegin(ScaleGestureDetector det) { return true; }

    public boolean onScale(ScaleGestureDetector det) {
        if (bitmap != null) {
            float focusX = det.getFocusX();
            float focusY = det.getFocusY();
            float scaleFactor = det.getScaleFactor();
            float pageFocusX = (focusX + scrollX) / viewScale;
            float pageFocusY = (focusY + scrollY) / viewScale;
            viewScale *= scaleFactor;
            if (viewScale < minScale) viewScale = minScale;
            if (viewScale > maxScale) viewScale = maxScale;
            bitmapW = (int)(bitmap.getWidth() * viewScale);
            bitmapH = (int)(bitmap.getHeight() * viewScale);
            scrollX = (int)(pageFocusX * viewScale - focusX);
            scrollY = (int)(pageFocusY * viewScale - focusY);
            scroller.forceFinished(true);
            invalidate();
        }
        return true;
    }

    public void onScaleEnd(ScaleGestureDetector det) { }

    public void goBackward() {

    }

    public void goForward() {

    }

    public void onDraw(Canvas canvas) {
        int x, y;

        if (bitmap == null) {
            if (error) {
                canvas.translate(canvasW / 2, canvasH / 2);
                canvas.drawPath(errorPath, errorPaint);
            }
            return;
        }

        if (scroller.computeScrollOffset()) {
            scrollX = scroller.getCurrX();
            scrollY = scroller.getCurrY();
            invalidate(); /* keep animating */
        }

        if (bitmapW <= canvasW) {
            scrollX = 0;
            x = (canvasW - bitmapW) / 2;
        } else {
            if (scrollX < 0) scrollX = 0;
            if (scrollX > bitmapW - canvasW) scrollX = bitmapW - canvasW;
            x = -scrollX;
        }

        if (bitmapH <= canvasH) {
            scrollY = 0;
            y = (canvasH - bitmapH) / 2;
        } else {
            if (scrollY < 0) scrollY = 0;
            if (scrollY > bitmapH - canvasH) scrollY = bitmapH - canvasH;
            y = -scrollY;
        }

        canvas.translate(x, y);
        canvas.scale(viewScale, viewScale);
        canvas.drawBitmap(bitmap, 0, 0, null);

        if (showLinks && links != null && links.length > 0) {
            for (Link link : links) {
                Rect b = link.bounds;
                canvas.drawRect(b.x0, b.y0, b.x1, b.y1, linkPaint);
            }
        }

        if (hits != null && hits.length > 0)
            for (Quad q : hits)
            {
                Path path = new Path();
                path.moveTo(q.ul_x, q.ul_y);
                path.lineTo(q.ll_x, q.ll_y);
                path.lineTo(q.lr_x, q.lr_y);
                path.lineTo(q.ur_x, q.ur_y);
                path.close();

                canvas.drawPath(path, hitPaint);
            }
    }
}