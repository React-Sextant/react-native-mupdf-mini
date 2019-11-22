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
import { View,Button } from 'react-native'
import MuPdfView from 'react-native-mupdf-mini'

export default class extends React.Component {
    static defaultProps = {
        page:Number,
        scale:Number,
        minScale:Number,
        maxScale:Number,
    };

    search=()=>{
        this.refs.mupdfview.search(1,1,"T")
    };

    setPage=(page)=>{
        this.refs.setPage(page)
    };

    getOutLine=async ()=>{
        return await this.refs.mupdfview.getOutLine()
    };

    render(){
        return (
            <View style={{flex:1}}>
                <MuPdfView
                    ref="mupdfview"
                    style={{flex:1}}
                    path={"/storage/emulated/0/Download/pdf_t2.pdf"}
                    page={1}
                    scale={1}
                    minScale={1}
                    maxScale={3}
                    pageScale={1}
                    onLoadComplete={(numberOfPages, path, {width, height}, tableContents, outline)=>{

                    }}
                    onPageChanged={(page,numberOfPages)=>{}}
                    onToggleUI={()=>{}}
                    onScaleChanged={(scale)=>{}}
                    onPressLink={(uri)=>{}}
                    onError={(error)=>{}}
                    onChange={(event)=>{
                        let message = event.nativeEvent.message.split('|');
                        console.log(message)
                    }}
                />
                <View>
                    <Button title={"search"} onPress={this.search}/>
                </View>
            </View>
        )
    }
}

```
