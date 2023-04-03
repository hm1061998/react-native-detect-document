/* eslint-disable prettier/prettier */
import CropperView from './components/CropperView';
import type {
  DocumentCorrersResults,
  CropperResults,
  getImageResults,
  RotateResults,
} from './types';
import {
  detectFile,
  cropImage,
  getResultImage,
  rotateImage,
  cleanText,
  resizeImage,
} from './helpers/detectorAndCropper';

const RnDetectDocument = {
  CropperView,
  detectFile,
  cropImage,
  getResultImage,
  rotateImage,
  cleanText,
  resizeImage,
};

export {
  CropperView,
  detectFile,
  cropImage,
  getResultImage,
  rotateImage,
  cleanText,
  resizeImage,
};
export type {
  DocumentCorrersResults,
  CropperResults,
  getImageResults,
  RotateResults,
};
export default RnDetectDocument;
