/* eslint-disable prettier/prettier */
import type { ColorValue } from 'react-native';

export declare type NumberProp = string | number;

export type CropperHandle = {
  crop: () => void;
};

export interface DocumentCorrersResults {
  corners: Object;
  width: number;
  height: number;
}

export interface CropperResults {
  image: string;
}

export interface RotateResults extends CropperResults {
  width: number;
  height: number;
}

export interface PointProps {
  x: number;
  y: number;
}

export interface RectangleProps {
  topLeft: PointProps;
  topRight: PointProps;
  bottomLeft: PointProps;
  bottomRight: PointProps;
}

interface DataImageResult extends RectangleProps {
  height: number;
  width: number;
}

export interface CropperViewProps {
  width: number;
  height: number;
  initialImage: string;
  rectangleCoordinates: RectangleProps;
  overlayColor: string;
  overlayOpacity: string;
  overlayStrokeColor: string;
  overlayStrokeWidth: string;
  handlerColor: string;
  updateImage?: (image: string, points: DataImageResult) => void;
  onHander: (key: string) => void;
}

export interface CropperProps extends CropperViewProps {
  layout: { width: number; height: number };
}

export interface PolygonProps {
  points?: number[][];
  fill?: ColorValue;
  fillOpacity?: NumberProp;
  stroke?: ColorValue;
  strokeWidth?: NumberProp;
}

export interface getImageResults {
  image: string;
  blur: string;
  candy: string;
  newImage: string;
}
