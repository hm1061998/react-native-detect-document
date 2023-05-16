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
  path: string,
  isCard: boolean = false
): Promise<DocumentCorrersResults | never> => {
  return RnDDM.detectFile(path, isCard);
};

export const cropImage = (
  path: string,
  points: Object,
  quality: number = 100,
  rotateDeg: number = 0
): Promise<CropperResults | never> => {
  return RnDDM.cropImage(path, points, quality, rotateDeg);
};

export const rotateImage = (
  path: string,
  isClockwise: boolean = true
): Promise<RotateResults | never> => {
  return RnDDM.rotateImage(path, isClockwise);
};

export const getResultImage = (
  path: string
): Promise<getImageResults | never> => {
  return RnDDM.getResultImage(path);
};

export const cleanText = (path: string): Promise<getImageResults | never> => {
  return RnDDM.cleanText(path);
};

export const resizeImage = (
  path: string,
  options: { width: number; height: number; quality: number }
): Promise<{ uri: string } | never> => {
  const newOptions = {
    ...options,
    quality: options.quality || 100,
  };
  return RnDDM.resizeImage(path, newOptions);
};
