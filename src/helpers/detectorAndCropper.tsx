/* eslint-disable prettier/prettier */
import { NativeModules, Platform } from 'react-native';
import type {
  DocumentCorrersResults,
  CropperResults,
  getImageResults,
  RotateResults,
} from '../types';

const LINKING_ERROR =
  `The package 'react-native-detect-document' doesn't seem to be linked. Make sure: \n\n` +
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

export const detectFile = (
  path: string
): Promise<DocumentCorrersResults | never> => {
  return RnDDM.detectFile(path);
};

export const cropImage = (
  path: string,
  points: Object,
  quality?: number
): Promise<CropperResults | never> => {
  return RnDDM.cropImage(path, points, quality || 100);
};

export const rotateImage = (
  path: string,
  isClockwise?: boolean
): Promise<RotateResults | never> => {
  return RnDDM.rotateImage(path, isClockwise || true);
};

export const getResultImage = (
  path: string
): Promise<getImageResults | never> => {
  return RnDDM.getResultImage(path);
};
