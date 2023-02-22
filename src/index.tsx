/* eslint-disable prettier/prettier */
import CropperView from './components/CropperView';
import type {
  DocumentCorrersResults,
  CropperResults,
  getImageResults,
} from './types';
import {
  findDocumentCorrers,
  cropper,
  getResultImage,
} from './helpers/detectorAndCropper';

const RnDetectDocument = {
  CropperView,
  findDocumentCorrers,
  cropper,
  getResultImage,
};

export { CropperView, findDocumentCorrers, cropper, getResultImage };
export type { DocumentCorrersResults, CropperResults, getImageResults };
export default RnDetectDocument;
