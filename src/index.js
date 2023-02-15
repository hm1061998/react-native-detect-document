


import { NativeModules, Platform } from 'react-native';
import CropperView from './CropperView';


const LINKING_ERROR =
  `The package 'rn-detect-document' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo Go\n';

const RnDetectDocument = NativeModules.RnDetectDocument
  ? NativeModules.RnDetectDocument
  : new Proxy(
    {},
    {
      get() {
        throw new Error(LINKING_ERROR);
      },
    }
  );

function findDocumentCorrers(path) {
  return RnDetectDocument.findDocumentCorrers(path);
}

function cropper(path, points, quality = 100) {
  return RnDetectDocument.cropper(path, points, quality);
}

export default {
  CropperView,
  findDocumentCorrers,
  cropper
};
