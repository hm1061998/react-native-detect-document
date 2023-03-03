/* eslint-disable prettier/prettier */
import CropperView from './components/CropperView';
import type {
  DocumentCorrersResults,
  CropperResults,
  getImageResults,
} from './types';
import {
  detectFile,
  cropImage,
  getResultImage,
} from './helpers/detectorAndCropper';

const RnDetectDocument = {
  CropperView,
  detectFile,
  cropImage,
  getResultImage,
};

export { CropperView, detectFile, cropImage, getResultImage };
export type { DocumentCorrersResults, CropperResults, getImageResults };
export default RnDetectDocument;
