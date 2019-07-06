'use strict';
import React, {Component} from 'react';
import PropTypes from 'prop-types';
import {
    requireNativeComponent,
    View,
    Platform,
    ProgressBarAndroid,
    ProgressViewIOS,
    ViewPropTypes,
} from 'react-native';

export default class MuPdf extends Component {

    static propTypes = {
        ...ViewPropTypes,
        source: PropTypes.oneOfType([
            PropTypes.shape({
                uri: PropTypes.string,
                cache: PropTypes.bool,
                expiration: PropTypes.number,
            }),
            // Opaque type returned by require('./test.pdf')
            PropTypes.number,
        ]).isRequired,
        page: PropTypes.number,
        scale: PropTypes.number,
        minScale: PropTypes.number,
        maxScale: PropTypes.number,
        horizontal: PropTypes.bool,
        spacing: PropTypes.number,
        password: PropTypes.string,
        progressBarColor: PropTypes.string,
        activityIndicator: PropTypes.any,
        activityIndicatorProps: PropTypes.any,
        enableAntialiasing: PropTypes.bool,
        enableAnnotationRendering: PropTypes.bool,
        enablePaging: PropTypes.bool,
        enableRTL: PropTypes.bool,
        fitPolicy: PropTypes.number,
        onLoadComplete: PropTypes.func,
        onPageChanged: PropTypes.func,
        onError: PropTypes.func,
        onPageSingleTap: PropTypes.func,
        onScaleChanged: PropTypes.func,

        // Props that are not available in the earlier react native version, added to prevent crashed on android
        accessibilityLabel: PropTypes.string,
        importantForAccessibility: PropTypes.string,
        renderToHardwareTextureAndroid: PropTypes.string,
        testID: PropTypes.string,
        onLayout: PropTypes.bool,
        accessibilityLiveRegion: PropTypes.string,
        accessibilityComponentType: PropTypes.string,
    };

    static defaultProps = {
        password: "",
        scale: 1,
        minScale: 1,
        maxScale: 3,
        spacing: 10,
        fitPolicy: 2, //fit both
        horizontal: false,
        page: 1,
        enableAntialiasing: true,
        enableAnnotationRendering: true,
        enablePaging: false,
        enableRTL: false,
        activityIndicatorProps: {color: '#009900', progressTintColor: '#009900'},
        onLoadProgress: (percent) => {
        },
        onLoadComplete: (numberOfPages, path) => {
        },
        onPageChanged: (page, numberOfPages) => {
        },
        onError: (error) => {
        },
        onPageSingleTap: (page) => {
        },
        onScaleChanged: (scale) => {
        },
    };

    constructor(props) {

        super(props);
        this.state = {
            path: "/storage/emulated/0/Download/4.pdf",
            isDownloaded: false,
            progress: 0,
            isSupportPDFKit: -1
        };

        this.lastRNBFTask = null;

    }







    _onChange = (event) => {

        let message = event.nativeEvent.message.split('|');
        // //__DEV__ && console.log("onChange: " + message);
        // if (message.length > 0) {
        //     if (message.length > 5) {
        //         message[4] = message.splice(4).join('|');
        //     }
        //     if (message[0] === 'loadComplete') {
        //         this.props.onLoadComplete && this.props.onLoadComplete(Number(message[1]), this.state.path, {
        //                 width: Number(message[2]),
        //                 height: Number(message[3]),
        //             },
        //             message[4]&&JSON.parse(message[4]));
        //     } else if (message[0] === 'pageChanged') {
        //         this.props.onPageChanged && this.props.onPageChanged(Number(message[1]), Number(message[2]));
        //     } else if (message[0] === 'error') {
        //         this._onError(new Error(message[1]));
        //     } else if (message[0] === 'pageSingleTap') {
        //         this.props.onPageSingleTap && this.props.onPageSingleTap(message[1]);
        //     } else if (message[0] === 'scaleChanged') {
        //         this.props.onScaleChanged && this.props.onScaleChanged(message[1]);
        //     }
        // }

    };

    _onError = (error) => {

        this.props.onError && this.props.onError(error);

    };

    render() {
        return (
            <PdfCustom
                ref={component => (this._root = component)}
                {...this.props}
                style={[{flex:1}, this.props.style]}
                path={this.state.path}
                onChange={this._onChange}
            />
        )
    }
}


if (Platform.OS === "android") {
    var PdfCustom = requireNativeComponent("RCTMuPdf")
}
