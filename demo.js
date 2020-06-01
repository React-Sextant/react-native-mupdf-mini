import React from 'react';
import {View, Button} from 'react-native';
import MuPdfMini from 'react-native-mupdf-mini';

export default class extends React.Component {
    static defaultProps = {
        page: Number,
        scale: Number,
        minScale: Number,
        maxScale: Number,
    };

    search = () => {
        this.refs.mupdfview.search(1, 1, 'T');
    };

    delete = () => {
        this.refs.mupdfview.deleteAnnotation(1);
    };

    setPage = page => {
        this.refs.setPage(page);
    };

    getOutLine = async () => {
        return await this.refs.mupdfview.getOutLine();
    };

    render() {
        return (
            <View style={{flex: 1}}>
    <MuPdfMini
        ref="mupdfview"
        style={{flex: 1}}
        path={'/storage/emulated/0/Download/pdf_t2.pdf'}
        page={1}
        scale={1}
        minScale={1}
        maxScale={3}
        pageScale={1}
        onLoadComplete={(
            numberOfPages,
            path,
            {width, height},
            tableContents,
            outline,
        ) => {}}
        onPageChanged={(page, numberOfPages) => {}}
        onToggleUI={() => {}}
        onScaleChanged={scale => {}}
        onPressLink={uri => {}}
        onError={error => {}}
        onChange={event => {
            let message = event.nativeEvent.message.split('|');
            console.log(message);
        }}
        />
        <View>
        <Button title={'search'} onPress={this.search} />
        <Button title={'delete'} onPress={this.delete} />
        <Button title={'add'} onPress={this.addOtherAnnotation} />
        </View>
        </View>
    );
    }

    addInkAnnotation = () => {
        let lines = [
            [
                {x: 130.56944, y: 229.73608},
                {x: 132.74976, y: 228.65765},
                {x: 143.03616, y: 222.1706},
                {x: 172.17484, y: 206.23047},
                {x: 200.92744, y: 191.18939},
                {x: 228.18274, y: 177.07849},
                {x: 250.6713, y: 163.90045},
            ],
            [
                {x: 136.62962, y: 266.09723},
                {x: 139.9462, y: 264.9135},
                {x: 166.45085, y: 254.2716},
                {x: 197.21745, y: 241.9123},
                {x: 219.97903, y: 231.19733},
                {x: 238.27547, y: 220.09491},
            ],
            [
                {x: 184.00926, y: 147.09723},
                {x: 183.71062, y: 152.38007},
                {x: 183.2121, y: 197.09802},
                {x: 185.4519, y: 241.66162},
                {x: 189.6893, y: 270.10938},
                {x: 191.72221, y: 281.79858},
            ],
            [
                {x: 232.49074, y: 113.49072},
                {x: 232.18715, y: 142.44305},
                {x: 232.25438, y: 193.28632},
                {x: 234.24693, y: 226.6731},
                {x: 237.17361, y: 252.3241},
            ],
        ];
        let obj = {
            type: 'drawing',
            lines: lines,
            width: 7,
            color: 'red',
        };

        this.refs.mupdfview.addAnnotation(JSON.stringify(obj));
    };

    addOtherAnnotation = () => {
        let hits = [
            {
                ll_x: 477.48395,
                ll_y: 441.44803,
                lr_x: 133.8,
                lr_y: 441.44803,
                ul_x: 133.8,
                ul_y: 452.0967,
                ur_x: 477.48395,
                ur_y: 452.0967,
            },
            {
                ll_x: 467.28937,
                ll_y: 453.44803,
                lr_x: 133.79991,
                lr_y: 453.44803,
                ul_x: 133.79991,
                ul_y: 464.0967,
                ur_x: 467.28937,
                ur_y: 464.0967,
            },
        ];
        let obj = {
            type: 'strikeout',
            hits: hits,
        };

        this.refs.mupdfview.addAnnotation(JSON.stringify(obj));
    };
}
