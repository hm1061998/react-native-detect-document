import type { PointProps, OptionsView, ShareValueXYProps } from '../types';
//calc position of point in view container
export const imageCoordinatesToViewCoordinates = (
  corner: PointProps,
  options: OptionsView
) => {
  const { viewWidth, viewHeight, width, height } = options;
  const x = corner.x * (viewWidth / width);
  const y = corner.y * (viewHeight / height);
  return {
    x,
    y,
  };
};

export const updateCoordinates = (
  target: ShareValueXYProps,
  rectangleCoordinate: PointProps,
  options: OptionsView
) => {
  console.log('target', target);

  const data = imageCoordinatesToViewCoordinates(rectangleCoordinate, options);
  target.x.value = data.x;
  target.y.value = data.y;
};
