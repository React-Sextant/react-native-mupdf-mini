package com.github.ReactSextant.mupdfmini;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.annotations.ReactProp;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

public class RCTMuPdfManager extends SimpleViewManager<MuPdfView> {
    private static final String REACT_CLASS = "RCTMuPdfMini";

    public static final int COMMAND_SEARCH = 1;
    public static final int COMMAND_RESET_SEARCH = 2;

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

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void onAfterUpdateTransaction(MuPdfView mupdfView) {
        if(mupdfView.isLayoutDirectionResolved())
            mupdfView.render();
    }

    @Override
    public Map<String,Integer> getCommandsMap() {
        Map<String, Integer> map = new HashMap<>();

        map.put("search", COMMAND_SEARCH);
        map.put("resetSearch", COMMAND_RESET_SEARCH);

        return map;
    }

    @Override
    public void receiveCommand(MuPdfView mupdfView, int commandType, @Nullable ReadableArray args){
        switch (commandType) {
            case COMMAND_SEARCH: {
                mupdfView.search(args.getInt(0),args.getInt(1),args.getString(2));
                return;
            }
            case COMMAND_RESET_SEARCH: {
                mupdfView.resetSearch();
                return;
            }
            default:
                throw new IllegalArgumentException(String.format(
                        "Unsupported command %d received by %s.",
                        commandType,
                        getClass().getSimpleName()));
        }
    }

    @ReactProp(name = "path")
    public void setPath(MuPdfView mupdfView, String path) {
        mupdfView.setPath(path);
    }

    @ReactProp(name = "page")
    public void setPage(MuPdfView mupdfView, int page) {
        mupdfView.setPage(page);
    }


    @ReactProp(name = "scale")
    public void setScale(MuPdfView mupdfView, float scale) {
        mupdfView.setScale(scale);
    }

    @ReactProp(name = "minScale")
    public void setMinScale(MuPdfView mupdfView, float minScale) {
        mupdfView.setMinScale(minScale);
    }

    @ReactProp(name = "maxScale")
    public void setMaxScale(MuPdfView mupdfView, float maxScale) {
        mupdfView.setMaxScale(maxScale);
    }

    @ReactProp(name = "password")
    public void setPassword(MuPdfView mupdfView, String password) {
        mupdfView.setPassword(password);
    }
}
