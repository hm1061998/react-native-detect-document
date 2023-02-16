/* eslint-disable prettier/prettier */
import { NativeModules, Platform } from 'react-native';
import type { DocumentCorrersResults, CropperResults } from '../types';

const LINKING_ERROR =
  `The package 'rn-detect-document' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo Go\n';

const RnDDM = NativeModules.RnDetectDocument
  ? NativeModules.RnDetectDocument
  : new Proxy(
      {},
      {
        get() {
          throw new Error(LINKING_ERROR);
        },
      }
    );

export const findDocumentCorrers = (
  path: string
): Promise<DocumentCorrersResults | never> => {
  return RnDDM.findDocumentCorrers(path);
};

export const cropper = (
  path: string,
  points: Object,
  quality: number = 100
): Promise<CropperResults | never> => {
  return RnDDM.cropper(path, points, quality);
};
