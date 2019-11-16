# react-native-mupdf
mupdf-android-viewer-mini in react native view manager

## Manual configuration
android/build.gradle
```bash
allprojects {
	repositories {
	    ...
	    maven { url 'http://maven.ghostscript.com/' } //add here
	}
}
```

## Usage
```jsx harmony
import React from 'react'
import { View } from 'react-native'
import MuPdfView from 'react-native-mupdf-mini'

export default class extends React.Component {
    static defaultProps = {
        page:Number,
        scale:Number,
        minScale:Number,
        maxScale:Number,
    };
    
    search=(value,type)=>{
        this.refs.mupdfview(value,type)
    };

    setPage=(page)=>{
        this.refs.setPage(page)
    };

    getOutLine=async ()=>{
        return await this.refs.mupdfview.getOutLine()
    };

    goForward=()=>{
        this.refs.mupdfview.goForward()
    };

    goBackward=()=>{
        this.refs.mupdfview.goBackward()
    };

    render(){
        return (
            <View style={{flex:1}}>
                <MuPdfView
                    ref="mupdfview"
                    style={{flex:1}}
                    path={"/storage/emulated/0/Download/pdf_t2.pdf"}

                    onToggleUI={()=>{}}
                    onPageSingleTap={(page)=>{}}
                    onScaleChanged={(scale)=>{}}
                    onPressLink={(uri)=>{}}
                    onPageChanged={(page,numberOfPages)=>{}}
                    onError={(error)=>{}}
                />
            </View>
        )
    }
}

```
