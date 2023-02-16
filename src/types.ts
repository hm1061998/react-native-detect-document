/* eslint-disable prettier/prettier */
import type { ColorValue } from 'react-native';

export declare type NumberProp = string | number;

export interface DocumentCorrersResults {
  corners: Object;
  width: number;
  height: number;
}

export interface CropperResults {
  image: string;
}

export interface CropperViewProps {
  width: number;
  height: number;
  initialImage: string;
  rectangleCoordinates: {
    topLeft: { x: number; y: number };
    topRight: { x: number; y: number };
    bottomLeft: { x: number; y: number };
    bottomRight: { x: number; y: number };
  };
  overlayColor: string;
  overlayOpacity: string;
  overlayStrokeColor: string;
  overlayStrokeWidth: string;
  handlerColor: string;
  updateImage: (
    image: string,
    points: {
      topLeft: { x: number; y: number };
      topRight: { x: number; y: number };
      bottomLeft: { x: number; y: number };
      bottomRight: { x: number; y: number };
      height: number;
      width: number;
    }
  ) => void;
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

export type CropperHandle = {
  crop: () => void;
};
