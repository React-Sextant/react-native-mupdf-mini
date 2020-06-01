# react-native-mupdf
mupdf-android-viewer-mini in react native view manager

## Manual configuration
android/build.gradle
```bash
allprojects {
	repositories {
	    ...
	    maven { url 'http://maven.ghostscript.com/' } //add here

	    //国内下载不了就用这个镜像
	    maven { url 'https://luokun.oss-cn-hangzhou.aliyuncs.com/ghostscript' } // CHINA!
	}
}
```

## Usage
```jsx harmony
import React from 'react'
import { View,Button } from 'react-native'
import MuPdfMini from 'react-native-mupdf-mini'

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
                <MuPdfMini
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

## import com.artifex.mupdf.fitz.*;

### PDFPage.java

> PDFPage extends Page 

|props|description|
|----|----|
|getAnnotations||
|createAnnotations|mPDFPage.createAnnotation(PDFAnnotation.TYPE_INK).setInkList((float[][])lines);|
|deleteAnnotations||
|toStructuredText|获取StructuredText类|

### PDFDocument

> PDFDocument extends Document
> 用于编辑PDF的类

|props|description|
|----|----|
|`PDFObject` addImage(`Image` var1)|`PDFObject`:edit pdf|

### PDFAnnotation

> 墨迹批注:
> 相比StructuredText，是最简单的批注实现;
> 文本批注请是先获取`Quad`坐标，再通过`drawPath`渲染到canvas，详见官方search案例

|props|description|
|----|----|
|`static` TYPE|mPDFPage.getAnnotations()[i].getType()|
|TYPE_INK|15: 墨迹|
|TYPE_STRIKE_OUT|11: 删除线|
|TYPE_UNDERLINE|9: 下划线|
|TYPE_HIGHLIGHT|8: 高亮|
| | - |
|setInkList|mPDFPage.createAnnotation(PDFAnnotation.TYPE_INK).setInkList((float[][])lines);`only TYPE_INK`|
|setBorder|mPDFPage.getAnnotations()[0].setBorder(10);`only TYPE_INK`|
|setColor|mPDFPage.getAnnotations()[0].setColor(parseColor("#000000"));`only TYPE_INK`|

> parseColor()

```java
public static float[] parseColor(String str) {
    int color = Color.parseColor(str);
    int red = (color & 0xff0000) >> 16;
    int green = (color & 0x00ff00) >> 8;
    int blue = (color & 0x0000ff);

    float colors[] = new float[3];
    colors[0] = red/255f;
    colors[1] = green/255f;
    colors[2] = blue/255f;

    return colors;
}
```

### StructuredText

> page.toStructuredText() 仅文字类操作

|props|description|
|----|----|
|search|搜索指定字|
|highlight|高亮指定区域内的文字|
|snapSelection|单个字符、单个单词、下划线|
|copy|复制指定区域内的文字|

1. 序列化非墨迹批注
```java
Rect rect = mPDFPage.getAnnotations()[index].getRect();
Point point1 = new Point(rect.x0, rect.y0);
Point point2 = new Point(rect.x1, rect.y1);
hits = mPDFPage.toStructuredText().highlight(point1,point2);
```


### Quad
```json
{"hits": [
        {
          "ll_x": 133.8,
          "ll_y": 451.41068,
          "lr_x": 340.16888,
          "lr_y": 451.07193,
          "ul_x": 133.8,
          "ul_y": 441.44803,
          "ur_x": 340.16888,
          "ur_y": 441.78677
        },
        {
          "ll_x": 340.2,
          "ll_y": 452.0967,
          "lr_x": 344.16812,
          "lr_y": 452.0967,
          "ul_x": 340.2,
          "ul_y": 445.12985,
          "ur_x": 344.16812,
          "ur_y": 445.12985
        }
]}
```
|props|description|
|----|----|
|[ ll_x, ll_y ]|下划线|
|[ lr_x, lr_x ]|删除线|
|[ ul_x, ul_x ]||
|[ ur_x, ur_x ]||
