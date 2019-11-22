package com.github.ReactSextant.mupdfmini;

import android.content.Context;
import android.support.annotation.Nullable;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.annotations.ReactProp;

import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;

public class RCTMuPdfManager extends SimpleViewManager<MuPdfView> {
    private static final String REACT_CLASS = "RCTMuPdfMini";

    public static final int COMMAND_SEARCH = 1;

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


    @Override
    public Map<String,Integer> getCommandsMap() {
        Map<String, Integer> map = new HashMap<>();

        map.put("search", COMMAND_SEARCH);

        return map;
    }

    @Override
    public void receiveCommand(MuPdfView mupdfView, int commandType, @Nullable ReadableArray args){
        switch (commandType) {
            case COMMAND_SEARCH: {
                mupdfView.runSearch(args.getInt(0),args.getInt(1),args.getString(2));
            }
        }
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

    @ReactProp(name = "disabled")
    public void setDisabled(MuPdfView mupdfView, boolean disabled) {
        mupdfView.setDisabled(disabled);
    }

    @ReactProp(name = "scale")
    public void setScale(MuPdfView mupdfView, int page) {
        mupdfView.setScale((float)page);
    }

    @ReactProp(name = "minScale")
    public void setMinScale(MuPdfView mupdfView, int page) {
        mupdfView.setMinScale((float)page);
    }

    @ReactProp(name = "maxScale")
    public void setMaxScale(MuPdfView mupdfView, int page) {
        mupdfView.setMaxScale((float)page);
    }

    @ReactProp(name = "pageScale")
    public void setPageScale(MuPdfView mupdfView, int page) {
        mupdfView.setPageScale((float)page);
    }
}
