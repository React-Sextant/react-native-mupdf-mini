package com.github.ReactSextant.mupdfmini;

import com.artifex.mupdf.fitz.*;
import com.artifex.mupdf.fitz.android.AndroidDrawDevice;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.events.RCTEventEmitter;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.net.Uri;
import android.text.method.PasswordTransformationMethod;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.Scroller;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Stack;

public class MuPdfView extends View implements
        GestureDetector.OnGestureListener,
        ScaleGestureDetector.OnScaleGestureListener
{

    private ThemedReactContext context;

    private final String APP = "MuPDFMini";


    protected Worker worker;

    protected Document doc;

    protected String key;
    protected String path;
    protected String mimetype;
    protected byte[] buffer;

    protected boolean disabled = false; // disallow singleTapUp to change page
    protected boolean hasLoaded;
    protected boolean isReflowable;
    protected boolean fitPage;
    protected String title;
    protected float layoutW, layoutH, layoutEm;
    protected float displayDPI;
    protected int canvasW, canvasH;
    protected float pageZoom;

    protected EditText searchText;

    protected int pageCount;
    protected int currentPage;
    protected int searchHitPage;
    protected String searchNeedle;
    protected boolean stopSearch;
    protected Stack<Integer> history;
    protected boolean wentBack;

    protected float pageScale, viewScale, minScale, maxScale;
    protected Bitmap bitmap;
    protected int bitmapW, bitmapH;
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
    protected String outlineStringify;

    public MuPdfView(ThemedReactContext ctx, AttributeSet atts) {
        super(ctx, atts);

        this.context = ctx;

        worker = new Worker(ctx);
        worker.start();

        scroller = new Scroller(ctx);
        detector = new GestureDetector(ctx, this);
        scaleDetector = new ScaleGestureDetector(ctx, this);

        pageScale = 1;
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

    public void setError() {
        if (bitmap != null)
            bitmap.recycle();
        error = true;
        links = null;
        hits = null;
        bitmap = null;
        invalidate();
    }

    public void setBitmap(Bitmap b, float zoom, boolean wentBack, Link[] ls, Quad[] hs) {
        if (bitmap != null)
            bitmap.recycle();
        error = false;
        links = ls;
        hits = hs;
        bitmap = b;
        bitmapW = (int)(bitmap.getWidth() * viewScale / zoom);
        bitmapH = (int)(bitmap.getHeight() * viewScale / zoom);
        scroller.forceFinished(true);
        if (pageScale == zoom) {
            scrollX = wentBack ? bitmapW - canvasW : 0;
            scrollY = wentBack ? bitmapH - canvasH : 0;
        }
        pageScale = zoom;
        invalidate();
    }

    public void resetHits() {
        hits = null;
        invalidate();
    }

    public void onSizeChanged(int w, int h, int ow, int oh) {
        canvasW = w;
        canvasH = h;
        onPageViewSizeChanged(w, h);
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
        boolean foundLink = false;
        float x = e.getX();
        float y = e.getY();
        if (showLinks && links != null) {
            float dx = (bitmapW <= canvasW) ? (bitmapW - canvasW) / 2 : scrollX;
            float dy = (bitmapH <= canvasH) ? (bitmapH - canvasH) / 2 : scrollY;
            float mx = (x + dx) / viewScale;
            float my = (y + dy) / viewScale;
            for (Link link : links) {
                Rect b = link.bounds;
                if (mx >= b.x0 && mx <= b.x1 && my >= b.y0 && my <= b.y1) {
                    if (link.uri != null)
                        gotoURI(link.uri);
                    else if (link.page >= 0)
                        gotoPage(link.page);
                    foundLink = true;
                    break;
                }
            }
        }
        if (!foundLink) {
            float a = canvasW / 3;
            float b = a * 2;
            if (x <= a && !disabled) goBackward();
            if (x >= b && !disabled) goForward();
            if (x > a && x < b) toggleUI();
        }
        invalidate();
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

    public boolean onScaleBegin(ScaleGestureDetector det) {
        return true;
    }

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

    public void onScaleEnd(ScaleGestureDetector det) {
        onPageViewZoomChanged(viewScale);

        //TODO: send onScaleChanged event
        onScaleChanged(viewScale);
    }

    public void goBackward() {
        if (currentPage > 0) {
            wentBack = true;
            currentPage --;
            loadPage();
        }
    }

    public void goForward() {
        if (currentPage < pageCount - 1) {
            currentPage ++;
            loadPage();
        }
    }

    private android.graphics.Rect dst = new android.graphics.Rect();
    private Path _path = new Path();

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

        dst.set(x, y, x + bitmapW, y + bitmapH);
        canvas.drawBitmap(bitmap, null, dst, null);

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

    protected void loadPage() {
        final int pageNumber = currentPage;
        final float zoom = pageZoom;
        stopSearch = true;
        worker.add(new Worker.Task() {
            public Bitmap bitmap;
            public Link[] links;
            public Quad[] hits;
            public void work() {
                try {
                    Page page = doc.loadPage(pageNumber);
                    Matrix ctm;
                    if (fitPage)
                        ctm = AndroidDrawDevice.fitPage(page, canvasW, canvasH);
                    else
                        ctm = AndroidDrawDevice.fitPageWidth(page, canvasW);
                    links = page.getLinks();
                    if (links != null)
                        for (Link link : links)
                            link.bounds.transform(ctm);
                    if (searchNeedle != null) {
                        hits = page.search(searchNeedle);
                        if (hits != null)
                            for (Quad hit : hits)
                                hit.transform(ctm);
                    }
                    if (zoom != 1)
                        ctm.scale(zoom);
                    bitmap = AndroidDrawDevice.drawPage(page, ctm);
                } catch (Throwable x) {
                }
            }
            public void run() {
                if (bitmap != null)
                    setBitmap(bitmap, zoom, wentBack, links, hits);
                else
                    setError();
                wentBack = false;

                //TODO: send onPageChanged event
                onPageChanged(pageNumber, pageCount);
            }
        });
    }

    public void gotoPage(int p) {
        if (p >= 0 && p < pageCount && p != currentPage) {
            history.push(currentPage);
            currentPage = p;
            loadPage();
        }
    }

    /******************** set variable ****************************/

    public void setPath(String path) {
        this.path = path;

        File file = new File(path);
        Uri uri = Uri.fromFile(file);
        mimetype = "application/pdf";
        key = uri.toString();
        if (uri.getScheme().equals("file")) {
            title = uri.getLastPathSegment();
            path = uri.getPath();
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
                Log.e(APP, "setPath:"+x.toString());
                Toast.makeText(this.context, x.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    // page start from 1
    public void setPage(int page) {
        this.currentPage = page>1?page-1:0;
        loadPage();
    }

    public void setDisabled(boolean bool){
        disabled = bool;
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

    public void setPageScale(float scale){
        pageScale = scale;
    }

    public void onPageViewSizeChanged(int w, int h) {
        pageZoom = 1;
        canvasW = w;
        canvasH = h;
        layoutW = canvasW * 72 / displayDPI;
        layoutH = canvasH * 72 / displayDPI;
        if (!hasLoaded) {
            hasLoaded = true;
            openDocument();
        } else if (isReflowable) {
            relayoutDocument();
        } else {
            loadPage();
        }
    }

    public void onPageViewZoomChanged(float zoom) {
        if (zoom != pageZoom) {
            pageZoom = zoom;
            loadPage();
        }
    }

    protected void openDocument() {
        worker.add(new Worker.Task() {
            boolean needsPassword;
            public void work() {
                Log.i(APP, "open document");
                if (path != null)
                    doc = Document.openDocument(path);
                else
                    doc = Document.openDocument(buffer, mimetype);
                needsPassword = doc.needsPassword();
            }
            public void run() {
                if (needsPassword)
                    askPassword("R.string.dlog_password_message");
                else
                    loadDocument();
            }
        });
    }

    protected void askPassword(String message) {
        final EditText passwordView = new EditText(this.context);
        passwordView.setInputType(EditorInfo.TYPE_TEXT_VARIATION_PASSWORD);
        passwordView.setTransformationMethod(PasswordTransformationMethod.getInstance());

        AlertDialog.Builder builder = new AlertDialog.Builder(this.context);
        builder.setTitle("Password:");
        builder.setMessage(message);
        builder.setView(passwordView);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                checkPassword(passwordView.getText().toString());
            }
        });
        builder.create().show();
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
                    askPassword("R.string.dlog_password_retry");
            }
        });
    }

    protected void resetSearch() {
        stopSearch = true;
        searchHitPage = -1;
        searchNeedle = null;
        resetHits();
    }

    protected void runSearch(final int startPage, final int direction, final String needle) {
        stopSearch = false;
        searchNeedle = needle;
        worker.add(new Worker.Task() {
            int searchPage = startPage;
            public void work() {
                if (stopSearch || needle != searchNeedle)
                    return;
                for (int i = 0; i < 9; ++i) {
                    Log.i(APP, "search page " + searchPage);
                    Page page = doc.loadPage(searchPage);
                    Quad[] hits = page.search(searchNeedle);
                    page.destroy();
                    if (hits != null && hits.length > 0) {
                        searchHitPage = searchPage;
                        break;
                    }
                    searchPage += direction;
                    if (searchPage < 0 || searchPage >= pageCount)
                        break;
                }
            }
            public void run() {
                if (stopSearch || needle != searchNeedle) {
//                    pageLabel.setText((currentPage+1) + " / " + pageCount);
                } else if (searchHitPage == currentPage) {
                    loadPage();
                } else if (searchHitPage >= 0) {
//                    history.push(currentPage);
                    currentPage = searchHitPage;
                    loadPage();
                } else {
                    if (searchPage >= 0 && searchPage < pageCount) {
//                        pageLabel.setText((searchPage+1) + " / " + pageCount);
                        worker.add(this);
                    } else {
//                        pageLabel.setText((currentPage+1) + " / " + pageCount);
                        Log.i(APP, "search not found");
//                        Toast.makeText(DocumentActivity.this, getString(R.string.toast_search_not_found), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    protected void search(int direction) {
        int startPage;
        if (searchHitPage == currentPage)
            startPage = currentPage + direction;
        else
            startPage = currentPage;
        searchHitPage = -1;
        searchNeedle = searchText.getText().toString();
        if (searchNeedle.length() == 0)
            searchNeedle = null;
        if (searchNeedle != null)
            if (startPage >= 0 && startPage < pageCount)
                runSearch(startPage, direction, searchNeedle);
    }

    protected void loadDocument() {
        worker.add(new Worker.Task() {
            public void work() {
                try {
                    Log.i(APP, "load document");
                    String metaTitle = doc.getMetaData(Document.META_INFO_TITLE);
                    if (metaTitle != null)
                        title = metaTitle;
                    isReflowable = doc.isReflowable();
                    if (isReflowable) {
                        Log.i(APP, "layout document");
                        doc.layout(layoutW, layoutH, layoutEm);
                    }
                    pageCount = doc.countPages();
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

                //TODO: send loadComplete event
                loadComplete(pageCount);
            }
        });
    }

    protected void relayoutDocument() {
        worker.add(new Worker.Task() {
            public void work() {
                try {
                    long mark = doc.makeBookmark(currentPage);
                    Log.i(APP, "relayout document");
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
                loadOutline();
            }
        });
    }

    private void loadOutline() {
        worker.add(new Worker.Task() {
            private void flattenOutline(Outline[] outline, String indent) {
                for (Outline node : outline) {
                    if (node.title != null)
                        outlineStringify += "{\"title\":"+node.title+",\"page\":"+node.page+"}";
                    if (node.down != null)
                        outlineStringify += "{\"down\":"+node.down+"}";
                }
            }
            public void work() {
                Log.i(APP, "load outline");
                Outline[] outline = doc.loadOutline();
                if (outline != null) {
                    outlineStringify = "[";
                    flattenOutline(outline, "");
                }
            }
            public void run() {
                //TODO: send outline data
                if (outlineStringify != null){
                    outlineStringify+="]";
                }

            }
        });
    }

    public void toggleUI() {
        //TODO: send toggleUI event
        WritableMap event = Arguments.createMap();
        event.putString("message", "toggleUI");
        ReactContext reactContext = (ReactContext)this.getContext();
        reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(
                this.getId(),
                "topChange",
                event
        );
    }

    public void gotoURI(String uri) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET); // FLAG_ACTIVITY_NEW_DOCUMENT in API>=21
        try {
            context.startActivity(intent);
        } catch (Throwable x) {
            Log.e(APP, x.getMessage());
            Toast.makeText(context, x.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /************** React Native onChange ************************/

    public void onPageChanged(int page, int numberOfPages){
        WritableMap event = Arguments.createMap();
        event.putString("message", "pageChanged|"+page+"|"+numberOfPages);
        ReactContext reactContext = (ReactContext)this.getContext();
        reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(
                this.getId(),
                "topChange",
                event
        );
    }

    public void loadComplete(int numberOfPages) {
        WritableMap event = Arguments.createMap();
        event.putString("message", "loadComplete|"+numberOfPages+"|"+layoutW+"|"+layoutH);

        ReactContext reactContext = (ReactContext)this.getContext();
        reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(
                this.getId(),
                "topChange",
                event
        );
    }

    public void onScaleChanged(float scale){
        WritableMap event = Arguments.createMap();
        event.putString("message", "scaleChanged|"+scale);

        ReactContext reactContext = (ReactContext)this.getContext();
        reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(
                this.getId(),
                "topChange",
                event
        );
    }

}
