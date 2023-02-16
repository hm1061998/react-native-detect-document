/* eslint-disable prettier/prettier */
import CropperView from './components/CropperView';
import type { DocumentCorrersResults, CropperResults } from './types';
import { findDocumentCorrers, cropper } from './helpers/detectorAndCropper';

const RnDetectDocument = {
  CropperView,
  findDocumentCorrers,
  cropper,
};

export { CropperView, findDocumentCorrers, cropper };
export type { DocumentCorrersResults, CropperResults };
export default RnDetectDocument;
