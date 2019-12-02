package com.github.ReactSextant.mupdfmini;

import com.artifex.mupdf.fitz.*;
import com.artifex.mupdf.fitz.android.AndroidDrawDevice;
import com.facebook.react.uimanager.ThemedReactContext;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.Scroller;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class MuPdfView extends View implements
        GestureDetector.OnGestureListener,
        ScaleGestureDetector.OnScaleGestureListener
{

    private ThemedReactContext  context;

    private final String APP = "MuPDFView";

    private Worker worker;
    protected Scroller scroller;
    protected GestureDetector detector;
    protected ScaleGestureDetector scaleDetector;

    protected PDFDocument doc;
    protected PDFPage mPDFPage;

    protected String path;
    protected String mimetype;
    protected byte[] buffer;

    // pdf info
    protected int currentPage;
    protected int pageCount;
    protected String title;
    protected String author;
    protected String key;       //search key word
    protected String password;

    // view info
    protected int canvasW, canvasH;
    protected int bitmapW, bitmapH;
    protected int scrollX, scrollY;
    protected Bitmap bitmap;
    protected float pageZoom;

    public Link[] links;
    public Quad[] hits;
    protected Paint linkPaint;
    protected Paint hitPaint;

    protected float layoutW, layoutH, layoutEm;
    protected float displayDPI;
    protected boolean hasLoaded;
    protected boolean isReflowable;
    protected boolean fitPage;

    // set prop
    protected float pageScale, viewScale, minScale, maxScale;
    protected boolean showLinks = true;


    public MuPdfView(ThemedReactContext ctx, AttributeSet atts) {
        super(ctx, atts);

        this.context = ctx;

        worker = new Worker(ctx);
        worker.start();

        scroller = new Scroller(ctx);
        detector = new GestureDetector(ctx, this);
        scaleDetector = new ScaleGestureDetector(ctx, this);

        currentPage = 0;
        pageScale = 1;
        viewScale = 1;
        minScale = 1;
        maxScale = 2;

        linkPaint = new Paint();
        linkPaint.setARGB(32, 0, 0, 255);


        hitPaint = new Paint();
        hitPaint.setARGB(32, 255, 0, 0);
        hitPaint.setStyle(Paint.Style.FILL);

    }

    public void render() {
        if (hasLoaded) {
            loadPage();
        } else if (isReflowable) {
            relayoutDocument();
        } else {
            openDocument();
        }
    }

    /**
     * open document
     *
     * Determine whether a password is required
     * **/
    protected void openDocument() {
        worker.add(new Worker.Task() {
            boolean needsPassword;
            public void work() {
                Log.i(APP, "open document");
                if (path != null)
                    doc = (PDFDocument) PDFDocument.openDocument(path);
                else
                    doc = (PDFDocument) PDFDocument.openDocument(buffer, mimetype);
                needsPassword = doc.needsPassword();
            }
            public void run() {
                if (needsPassword)
                    askPassword();
                else
                    loadDocument();
            }
        });
    }

    /**
     * load document
     *
     * Get PDF file info:
     * @info title
     * @info author
     * @info pageCount
     * **/
    protected void loadDocument() {
        worker.add(new Worker.Task() {
            public void work() {
                try {
                    Log.i(APP, "load document");
                    title = doc.getMetaData(Document.META_INFO_TITLE);
                    author = doc.getMetaData(Document.META_INFO_AUTHOR);
                    pageCount = doc.countPages();
                    isReflowable = doc.isReflowable();
                    if (isReflowable) {
                        Log.i(APP, "layout document");
                        doc.layout(layoutW, layoutH, layoutEm);
                    }
                } catch (Throwable x) {
                    doc = null;
                    pageCount = 1;
                    currentPage = 0;
                    throw x;
                }
            }
            public void run() {
                if (currentPage < 0 || currentPage >= pageCount)
                    currentPage = 0;
                loadPage();
            }
        });
    }

    /**
     * relayout document
     * **/
    protected void relayoutDocument() {
        worker.add(new Worker.Task() {
            public void work() {
                try {
                    Log.i(APP, "relayout document");
                    long mark = doc.makeBookmark(currentPage);
                    doc.layout(layoutW, layoutH, layoutEm);
                    pageCount = doc.countPages();
                    currentPage = doc.findBookmark(mark);
                } catch (Throwable x) {
                    pageCount = 1;
                    currentPage = 0;
                    throw x;
                }
            }

            public void run() {
                loadPage();
            }
        });
    }

    /**
     * loadPage
     * **/
    protected void loadPage(){
        final float zoom = pageZoom;
        worker.add(new Worker.Task() {
            public void work() {
                try {
                    mPDFPage = (PDFPage) doc.loadPage(currentPage);
                    Matrix ctm;
                    if (fitPage)
                        ctm = AndroidDrawDevice.fitPage(mPDFPage, canvasW, canvasH);
                    else
                        ctm = AndroidDrawDevice.fitPageWidth(mPDFPage, canvasW);

                    links = mPDFPage.getLinks();
                    if (links != null)
                        for (Link link : links)
                            link.bounds.transform(ctm);

                    if(key != null)
                        hits = mPDFPage.search(key);
                        if (hits != null)
                            for (Quad hit : hits)
                                hit.transform(ctm);

                    if (zoom != 1)
                        ctm.scale(zoom);

                    bitmap = AndroidDrawDevice.drawPage(mPDFPage, ctm);
                }catch (Throwable x){
                    throw x;
                }
            }

            public void run() {
                if (bitmap != null) {

                    bitmapW = (int)(bitmap.getWidth() * viewScale / zoom);
                    bitmapH = (int)(bitmap.getHeight() * viewScale / zoom);
                    scroller.forceFinished(true);
                    pageScale = zoom;
                    invalidate();
                }

            }
        });
    }


    protected void askPassword(){
        if(password != null)
            checkPassword(password);
    }

    protected void checkPassword(final String password) {
        worker.add(new Worker.Task() {
            boolean passwordOkay;
            public void work() {
                Log.i(APP, "check password");
                passwordOkay = doc.authenticatePassword(password);
            }
            public void run() {
                if (passwordOkay)
                    loadDocument();
                else
                    askPassword();
            }
        });
    }


    /*********** set ***********/
    public void setPath(String path){
        this.path = path;

        File file = new File(path);
        Uri uri = Uri.fromFile(file);
        mimetype = "application/pdf";
        if (uri.getScheme().equals("file")) {
            title = uri.getLastPathSegment();
            this.path = uri.getPath();
        } else {
            title = uri.toString();
            try {
                InputStream stm = this.context.getContentResolver().openInputStream(uri);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buf = new byte[16384];
                int n;
                while ((n = stm.read(buf)) != -1)
                    out.write(buf, 0, n);
                out.flush();
                buffer = out.toByteArray();
            } catch (IOException x) {
                Toast.makeText(this.context, x.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void setPage(int page){
        currentPage = page;
    }

    public void setScale(float scale){
        viewScale = scale;
    }

    public void setMinScale(float scale){
        minScale = scale;
    }

    public void setMaxScale(float scale){
        maxScale = scale;
    }

    public void setPassword(String p){
        password = p;
    }

    /*********** command method ***********/

    /**
     * search
     *
     * @param startPage 开始搜索起始页
     * @param direction 1：向下一页搜索 -1：向上一页搜索
     * @param _key       关键词
     * **/
    public void search(final int startPage, final int direction, final String _key){
        key = _key;
        worker.add(new Worker.Task() {
            public void work() {

            }
            public void run() {
                loadPage();
            }
        });
    }

    public void resetSearch(){
        key = null;
        hits = null;
        invalidate();
        loadPage();
    }

    /**
     * addAnnotation
     *
     * @param type eg: {@link PDFAnnotation#TYPE_INK}
     * @param str  stringify(PDFAnnotation Data)
     *             TYPE_INK: { path:[ [x1, y1], [x2, y2] ] }
     * **/
    public void addAnnotation(int type, String str){
        try{
            switch(type) {
                case PDFAnnotation.TYPE_INK:
                    loadPage();
                    break;
                case PDFAnnotation.TYPE_STRIKE_OUT:
                    loadPage();
                    break;
                case PDFAnnotation.TYPE_UNDERLINE:
                    loadPage();
                    break;
                case PDFAnnotation.TYPE_HIGHLIGHT:
                    loadPage();
                    break;
            }
        }catch (Throwable x){
            throw x;
        }
    }

    /**
     * addAnnotation
     *
     * @param str 数据结构遵循 @github#https://github.com/instructure/pdf-annotate.js
     * **/
    public void addAnnotation(String str){
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = (JsonObject) jsonParser.parse(str);
        String annotation_type = jsonObject.get("type").getAsString();
        Gson gson = new Gson();
        try{
            switch(annotation_type) {
                case "drawing":
                    mPDFPage.createAnnotation(PDFAnnotation.TYPE_INK).setInkList(gson.fromJson(str, Drawing.class).lines);
                    mPDFPage.getAnnotations()[mPDFPage.getAnnotations().length-1].setColor(Drawing.parseColor(gson.fromJson(str, Drawing.class).color));
                    mPDFPage.getAnnotations()[mPDFPage.getAnnotations().length-1].setBorder(gson.fromJson(str, Drawing.class).width);
                    mPDFPage.update();
                    loadPage();
                    break;
            }
        }catch (Throwable x){
            throw x;
        }
    }

    /**
     * deleteAnnotation
     *
     * @param index : PDFAnnotation index
     * **/
    public void deleteAnnotation(int index){
        if(index >= 0 && mPDFPage.getAnnotations() != null && mPDFPage.getAnnotations().length > index)
            mPDFPage.deleteAnnotation(mPDFPage.getAnnotations()[index]);
            loadPage();
    }

    @Override
    public void onSizeChanged(int w, int h, int ow, int oh) {
        pageZoom = 1;
        canvasW = w;
        canvasH = h;
        layoutW = canvasW * 72 / displayDPI;
        layoutH = canvasH * 72 / displayDPI;
        render();
    }

    private android.graphics.Rect dst = new android.graphics.Rect();
    private android.graphics.Path _path = new android.graphics.Path();

    @Override
    protected void onDraw(Canvas canvas) {
        int x, y;

        if (bitmap == null) {
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

        dst.set(x, y, x + bitmapW, y + bitmapH);

        canvas.drawBitmap(bitmap, null, dst, null);

        if(!hasLoaded)
            hasLoaded = true;

        // render links
        if (showLinks && links != null && links.length > 0) {
            for (Link link : links) {
                Rect b = link.bounds;
                canvas.drawRect(
                        x + b.x0 * viewScale,
                        y + b.y0 * viewScale,
                        x + b.x1 * viewScale,
                        y + b.y1 * viewScale,
                        linkPaint
                );
            }
        }

        // render hits
        if (hits != null && hits.length > 0) {
            for (Quad q : hits) {
                _path.rewind();
                _path.moveTo(x + q.ul_x * viewScale, y + q.ul_y * viewScale);
                _path.lineTo(x + q.ll_x * viewScale, y + q.ll_y * viewScale);
                _path.lineTo(x + q.lr_x * viewScale, y + q.lr_y * viewScale);
                _path.lineTo(x + q.ur_x * viewScale, y + q.ur_y * viewScale);
                _path.close();
                canvas.drawPath(_path, hitPaint);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        detector.onTouchEvent(event);
        scaleDetector.onTouchEvent(event);
        return true;
    }

    @Override
    public boolean onDown(MotionEvent motionEvent) {
        scroller.forceFinished(true);
        return true;
    }

    @Override
    public void onShowPress(MotionEvent motionEvent) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent motionEvent) {
        return true;
    }

    @Override
    public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent1, float dx, float dy) {

        if (bitmap != null) {
            scrollX += (int)dx;
            scrollY += (int)dy;
            scroller.forceFinished(true);
            invalidate();
        }
        return true;
    }

    @Override
    public void onLongPress(MotionEvent motionEvent) {

    }

    @Override
    public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
        return true;
    }

    @Override
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
            bitmapW = (int)(bitmap.getWidth() * viewScale / pageScale);
            bitmapH = (int)(bitmap.getHeight() * viewScale / pageScale);
            scrollX = (int)(pageFocusX * viewScale - focusX);
            scrollY = (int)(pageFocusY * viewScale - focusY);
            scroller.forceFinished(true);
            invalidate();
        }
        return true;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector scaleGestureDetector) {
        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector scaleGestureDetector) {
        pageZoom = viewScale;
        loadPage();
    }
}

class Drawing {
    public String type;
    public float[][] lines;
    public String color = "#FF0000";
    public int width = 3;

    public static float[] parseColor(String str) {
        int color = Color.parseColor(str);
        int red = (color & 0xff0000) >> 16;
        int green = (color & 0x00ff00) >> 8;
        int blue = (color & 0x0000ff);

        float colors[] = new float[3];
        colors[0] = red/255f;
        colors[1] = green/255f;
        colors[2] = blue/255f;

        return colors;
    }
}