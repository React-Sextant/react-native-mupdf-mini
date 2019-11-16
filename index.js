import React from 'react'
import {View, requireNativeComponent, findNodeHandle, NativeModules} from 'react-native'

const MuPdfView = requireNativeComponent("RCTMuPdfMini");
const MuPdfModule = NativeModules.MuPdfMiniModule;

export default class extends React.Component {
    static defaultProps = {
        page:Number,
        scale:Number,
        minScale:Number,
        maxScale:Number,
    };

    _setReference = (ref) => {
        if (ref) {
            this._handle = findNodeHandle(ref);
        } else {
            this._handle = null;
        }
    };

    /**
     * Search text
     *
     * @param value
     * @param type 1:search next, -1:search forward
     *
     * **/
    search=(value,type)=>{
        MuPdfModule.search(this._handle,value,type);
    };

    resetSearch=()=>{
        MuPdfModule.resetSearch(this._handle)
    }

    setPage=(page)=>{
        MuPdfModule.setPage(this._handle,page)
    };

    getOutLine=async ()=>{
        return await MuPdfModule.getOutLine(this._handle)
    };

    goForward=()=>{
        MuPdfModule.goForward(this._handle)
    };

    goBackward=()=>{
        MuPdfModule.goBackward(this._handle)
    };

    render(){
        return (
            <MuPdfView
                ref={this._setReference}
                {...this.props}
            />
    )
    }
}
