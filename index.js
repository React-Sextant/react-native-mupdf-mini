import React from 'react';
import {
  requireNativeComponent,
  findNodeHandle,
  UIManager,
} from 'react-native';

const MuPdfView = requireNativeComponent('RCTMuPdfMini');

export default class extends React.Component {
  static defaultProps = {
    page: Number,
    scale: Number,
    minScale: Number,
    maxScale: Number,
  };

  _setReference = ref => {
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
  search = (startPage, direction, needle) => {
    UIManager.dispatchViewManagerCommand(
        this._handle,
        UIManager.RCTMuPdfMini.Commands.search,
        [startPage, direction, needle],
    );
  };

  resetSearch = () => {
    UIManager.dispatchViewManagerCommand(
        this._handle,
        UIManager.RCTMuPdfMini.Commands.resetSearch,
        null,
    );
  };

  render() {
    return <MuPdfView ref={this._setReference} {...this.props} />;
  }

  addAnnotation = (annotation) => {
    UIManager.dispatchViewManagerCommand(
        this._handle,
        UIManager.RCTMuPdfMini.Commands.addAnnotation,
        [annotation],
    );
  };

  deleteAnnotation = index => {
    UIManager.dispatchViewManagerCommand(
        this._handle,
        UIManager.RCTMuPdfMini.Commands.deleteAnnotation,
        [index],
    );
  };
}
