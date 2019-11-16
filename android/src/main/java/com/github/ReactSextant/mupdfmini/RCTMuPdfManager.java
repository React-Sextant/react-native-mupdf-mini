package com.github.ReactSextant.mupdfmini;

import android.content.Context;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.annotations.ReactProp;

import static java.lang.String.format;

public class RCTMuPdfManager extends SimpleViewManager<MuPdfView> {
    private static final String REACT_CLASS = "RCTMuPdfMini";
    private Context context;
    private MuPdfView mupdfView;


    public RCTMuPdfManager(ReactApplicationContext reactContext){
        this.context = reactContext;
    }

    @Override
    public String getName() {
        return REACT_CLASS;
    }

    @Override
    public MuPdfView createViewInstance(ThemedReactContext context) {
        this.mupdfView = new MuPdfView(context,null);
        return mupdfView;
    }

    @Override
    public void onDropViewInstance(MuPdfView mupdfView) {
        mupdfView = null;
    }

    @ReactProp(name = "path")
    public void setPath(MuPdfView mupdfView, String path) {
        mupdfView.setPath(path);
    }

    // page start from 1
    @ReactProp(name = "page")
    public void setPage(MuPdfView mupdfView, int page) {
        mupdfView.setPage(page);
    }
}
