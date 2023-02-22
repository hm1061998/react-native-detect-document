/* eslint-disable prettier/prettier */
import {
  useAnimatedGestureHandler,
  SharedValue,
  useSharedValue,
  useAnimatedStyle,
  runOnJS,
} from 'react-native-reanimated';
import type { PointProps } from '../types';

interface AnimatedStyle {
  x: SharedValue<number>;
  y: SharedValue<number>;
}

interface OptionsView {
  viewWidth: number;
  viewHeight: number;
  width: number;
  height: number;
}

//calc position of point in view container
const imageCoordinatesToViewCoordinates = (
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

export const useCustomAnimatedGestureHandler = (
  animated: AnimatedStyle,
  options: { viewWidth: number; viewHeight: number },
  callback?: (key: string) => void
) => {
  const { viewHeight, viewWidth } = options;
  return useAnimatedGestureHandler(
    {
      onStart: (_event: any, ctx: { translateX: any; translateY: any }) => {
        ctx.translateX = animated.x.value;
        ctx.translateY = animated.y.value;
        if (callback) {
          runOnJS(callback)('start');
        }
      },
      onActive: (
        event: { translationX: any; translationY: any },
        ctx: { translateX: any; translateY: any }
      ) => {
        const offsetX = ctx.translateX;
        const offsetY = ctx.translateY;
        const x = offsetX + event.translationX;
        const y = offsetY + event.translationY;

        //limit area drag
        if (x >= 0 && y >= 0 && x <= viewWidth && y <= viewHeight) {
          animated.x.value = ctx.translateX + event.translationX;
          animated.y.value = ctx.translateY + event.translationY;
        }
      },
      onEnd: () => {
        if (callback) {
          runOnJS(callback)('end');
        }
      },
      onCancel: () => {
        if (callback) {
          runOnJS(callback)('cancel');
        }
      },
      onFail: () => {
        if (callback) {
          runOnJS(callback)('fail');
        }
      },
      onFinish: () => {
        if (callback) {
          runOnJS(callback)('finish');
        }
      },
    },
    []
  );
};

export const useSharedValueXY = (
  rectangleCoordinate: PointProps,
  options: OptionsView,
  defaultValue: PointProps
) => {
  const data = rectangleCoordinate
    ? imageCoordinatesToViewCoordinates(rectangleCoordinate, options)
    : defaultValue;
  return {
    x: useSharedValue(data.x),
    y: useSharedValue(data.y),
  };
};

export const useTranslateAnimatedStyle = (animated: AnimatedStyle) => {
  return useAnimatedStyle(() => {
    return {
      transform: [
        {
          translateX: animated.x.value,
        },
        {
          translateY: animated.y.value,
        },
      ],
    };
  }, []);
};
