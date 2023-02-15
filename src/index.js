


import { NativeModules, Platform } from 'react-native';
import RNCropperView from './CropperView';


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

export function findDocumentCorrers(path) {
  return RnDetectDocument.findDocumentCorrers(path);
}

export function cropper(path, points, quality = 100) {
  return RnDetectDocument.cropper(path, points, quality);
}

export const CropperView = RNCropperView

export default {
  RNCropperView : CropperView,
  findDocumentCorrers,
  cropper,
};
