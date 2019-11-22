import React from 'react'
import {View, requireNativeComponent, findNodeHandle, NativeModules, UIManager} from 'react-native'

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
     * @param startPage Int
     * @param direction 1:search next, -1:search forward
     * @param needle String key word
     *
     * **/
    search=(startPage, direction, needle)=>{
        UIManager.dispatchViewManagerCommand(
            this._handle,
            UIManager.RCTMuPdfMini.Commands.search,
            [startPage, direction, needle]
        )
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

    render(){
        return (
            <MuPdfView
                ref={this._setReference}
                {...this.props}
            />
    )
    }
}
