import * as React from 'react';
import { Dimensions, Image, View, StyleSheet } from 'react-native';
import Svg, { Polygon } from 'react-native-svg';
import Animated, {
  useAnimatedProps,
  AnimateProps,
} from 'react-native-reanimated';
import {
  PanGestureHandler,
  GestureHandlerRootView,
} from 'react-native-gesture-handler';
import { cropImage } from '../helpers/detectorAndCropper';
import {
  useCustomAnimatedGestureHandler,
  useTranslateAnimatedStyle,
  useSharedValueXY,
} from '../helpers/hooks';
import type {
  CropperProps,
  PolygonProps,
  CropperHandle,
  ShareValueXYProps,
} from '../types';

const AnimatedPolygon = Animated.createAnimatedComponent(
  Polygon
) as React.ComponentClass<Animated.AnimateProps<PolygonProps>, any>;

const screenWidth = Dimensions.get('window').width;

//calc viewHeight from height container
const getViewHeight = (
  width: number,
  height: number,
  layout: { height: number; width: number }
) => {
  const imageRatio = width / height;
  let _viewHeight = 0;
  // if (height > width) {
  //   // if user takes the photo in portrait
  //   _viewHeight = Math.round(screenWidth / imageRatio);
  // } else {
  //   // if user takes the photo in landscape
  //   _viewHeight = Math.round(screenWidth * imageRatio);
  // }
  _viewHeight = Math.round(screenWidth / imageRatio);
  _viewHeight = Math.min(_viewHeight, layout.height);
  _viewHeight -= 50;

  return _viewHeight;
};

//calc viewWidth from viewHeight and ratio image
const getViewWidth = (width: number, height: number, viewHeight: number) => {
  const imageRatio = width / height;
  let _viewWidth = 0;
  // if (height > width) {
  //   // if user takes the photo in portrait
  //   _viewWidth = Math.round(viewHeight * imageRatio) + 50;
  // } else {
  //   // if user takes the photo in landscape
  //   _viewWidth = Math.round(viewHeight / imageRatio) + 50;
  // }
  _viewWidth = Math.round(viewHeight * imageRatio) + 50;
  _viewWidth -= 50;

  return _viewWidth;
};

const Cropper = React.forwardRef<CropperHandle, CropperProps>(
  (
    {
      width,
      height,
      layout,
      initialImage,
      rectangleCoordinates,
      overlayColor,
      overlayOpacity,
      overlayStrokeColor,
      overlayStrokeWidth,
      updateImage,
      handlerColor,
      onHander,
      enablePanStrict,
    }: CropperProps,
    ref
  ) => {
    const viewHeight = getViewHeight(width, height, layout);
    const viewWidth = getViewWidth(width, height, viewHeight);
    const isCrop = React.useRef(true);
    const topLeftPrev = React.useRef({ x: 0, y: 0 });
    const topRightPrev = React.useRef({ x: viewWidth, y: 0 });
    const bottomLeftPrev = React.useRef({ x: 0, y: viewHeight });
    const bottomRightPrev = React.useRef({ x: viewWidth, y: viewHeight });

    //calc points of document on screen
    const optionsView = { viewWidth, viewHeight, width, height };
    const topLeft = useSharedValueXY(
      rectangleCoordinates?.topLeft,
      optionsView,
      { x: 100, y: 100 }
    );

    const topRight = useSharedValueXY(
      rectangleCoordinates?.topRight,
      optionsView,
      { x: viewWidth - 100, y: 100 }
    );

    const bottomLeft = useSharedValueXY(
      rectangleCoordinates?.bottomLeft,
      optionsView,
      { x: 100, y: viewHeight - 100 }
    );

    const bottomRight = useSharedValueXY(
      rectangleCoordinates?.bottomRight,
      optionsView,
      { x: viewWidth - 100, y: viewHeight - 100 }
    );
    //end calc

    const viewCoordinatesToImageCoordinates = (corner: ShareValueXYProps) => {
      return {
        x: (corner.x.value / viewWidth) * width,
        y: (corner.y.value / viewHeight) * height,
      };
    };

    React.useImperativeHandle(ref, () => ({
      crop: async (quality?: number) => {
        const coordinates = {
          topLeft: viewCoordinatesToImageCoordinates(topLeft),
          topRight: viewCoordinatesToImageCoordinates(topRight),
          bottomLeft: viewCoordinatesToImageCoordinates(bottomLeft),
          bottomRight: viewCoordinatesToImageCoordinates(bottomRight),
          height: height,
          width: width,
        };

        const res = await cropImage(initialImage, coordinates, quality);
        updateImage?.(res.image, coordinates);
        return { ...res, coordinates };
      },
      toogleCropMode: () => {
        const newTopLeft = { x: topLeft.x.value, y: topLeft.y.value };
        const newTopRight = { x: topRight.x.value, y: topRight.y.value };
        const newBottomRight = {
          x: bottomRight.x.value,
          y: bottomRight.y.value,
        };
        const newBottomLeft = { x: bottomLeft.x.value, y: bottomLeft.y.value };

        topLeft.x.value = topLeftPrev.current.x;
        topLeft.y.value = topLeftPrev.current.y;
        topRight.x.value = topRightPrev.current.x;
        topRight.y.value = topRightPrev.current.y;
        bottomRight.x.value = bottomRightPrev.current.x;
        bottomRight.y.value = bottomRightPrev.current.y;
        bottomLeft.x.value = bottomLeftPrev.current.x;
        bottomLeft.y.value = bottomLeftPrev.current.y;

        topLeftPrev.current = newTopLeft;
        topRightPrev.current = newTopRight;
        bottomRightPrev.current = newBottomRight;
        bottomLeftPrev.current = newBottomLeft;

        isCrop.current = !isCrop.current;
      },
      getStatus: () => {
        return isCrop.current;
      },
    }));

    const onChange = (key: string) => {
      const coordinates = {
        topLeft: viewCoordinatesToImageCoordinates(topLeft),
        topRight: viewCoordinatesToImageCoordinates(topRight),
        bottomLeft: viewCoordinatesToImageCoordinates(bottomLeft),
        bottomRight: viewCoordinatesToImageCoordinates(bottomRight),
      };

      onHander?.(key, coordinates);
    };

    //create event listener drag point
    const panResponderTopLeft = useCustomAnimatedGestureHandler(
      topLeft,
      {
        viewWidth,
        viewHeight,
      },
      onChange
    );

    const panResponderTopRight = useCustomAnimatedGestureHandler(
      topRight,
      {
        viewWidth,
        viewHeight,
      },
      onChange
    );

    const panResponderBottomRight = useCustomAnimatedGestureHandler(
      bottomRight,
      {
        viewWidth,
        viewHeight,
      },
      onChange
    );

    const panResponderBottomLeft = useCustomAnimatedGestureHandler(
      bottomLeft,
      {
        viewWidth,
        viewHeight,
      },
      onChange
    );

    //create style of points
    const topLeftStyle = useTranslateAnimatedStyle(topLeft);
    const topRightStyle = useTranslateAnimatedStyle(topRight);
    const bottomRightStyle = useTranslateAnimatedStyle(bottomRight);
    const bottomLeftStyle = useTranslateAnimatedStyle(bottomLeft);

    //create polygon with 4 points
    const animatedPointsValues = [topLeft, topRight, bottomRight, bottomLeft];

    const animatedProps = useAnimatedProps<Partial<AnimateProps<PolygonProps>>>(
      () => ({
        points: animatedPointsValues.map((item) => {
          return [item.x.value, item.y.value];
        }),
      }),
      []
    );

    const handleColorStyle = handlerColor
      ? { backgroundColor: handlerColor }
      : {};

    return (
      <GestureHandlerRootView>
        <View style={[{ height: viewHeight, width: viewWidth }]}>
          <Image
            style={[{ flex: 1 }]}
            resizeMode="cover"
            source={{ uri: initialImage }}
          />
          <Svg
            height={viewHeight}
            width={viewWidth}
            viewBox={`0 0 ${viewWidth} ${viewHeight}`}
            style={{ position: 'absolute', left: 0, top: 0 }}
          >
            <AnimatedPolygon
              fill={overlayColor || 'blue'}
              fillOpacity={overlayOpacity || 0.5}
              stroke={overlayStrokeColor || 'blue'}
              strokeWidth={overlayStrokeWidth || 3}
              animatedProps={animatedProps}
            />
          </Svg>

          <PanGestureHandler
            enabled={enablePanStrict}
            onGestureEvent={panResponderTopLeft}
          >
            <Animated.View style={[s.handler, topLeftStyle]}>
              <View
                style={[s.handlerI, { left: -10, top: -10 }, handleColorStyle]}
              />
              <View
                style={[
                  s.handlerRound,
                  { left: 31, top: 31 },
                  handleColorStyle,
                ]}
              />
            </Animated.View>
          </PanGestureHandler>

          <PanGestureHandler
            enabled={enablePanStrict}
            onGestureEvent={panResponderTopRight}
          >
            <Animated.View style={[s.handler, topRightStyle]}>
              <View
                style={[s.handlerI, { left: 10, top: -10 }, handleColorStyle]}
              />
              <View
                style={[
                  s.handlerRound,
                  { right: 31, top: 31 },
                  handleColorStyle,
                ]}
              />
            </Animated.View>
          </PanGestureHandler>

          <PanGestureHandler
            enabled={enablePanStrict}
            onGestureEvent={panResponderBottomLeft}
          >
            <Animated.View style={[s.handler, bottomLeftStyle]}>
              <View
                style={[s.handlerI, { left: -10, top: 10 }, handleColorStyle]}
              />
              <View
                style={[
                  s.handlerRound,
                  { left: 31, bottom: 31 },
                  handleColorStyle,
                ]}
              />
            </Animated.View>
          </PanGestureHandler>

          <PanGestureHandler
            enabled={enablePanStrict}
            onGestureEvent={panResponderBottomRight}
          >
            <Animated.View style={[s.handler, bottomRightStyle]}>
              <View
                style={[s.handlerI, { left: 10, top: 10 }, handleColorStyle]}
              />
              <View
                style={[
                  s.handlerRound,
                  { right: 31, bottom: 31 },
                  handleColorStyle,
                ]}
              />
            </Animated.View>
          </PanGestureHandler>
        </View>
      </GestureHandlerRootView>
    );
  }
);

const s = StyleSheet.create({
  handlerI: {
    borderRadius: 0,
    height: 20,
    width: 20,
    backgroundColor: 'blue',
  },
  handlerRound: {
    width: 39,
    position: 'absolute',
    height: 39,
    borderRadius: 100,
    backgroundColor: 'blue',
  },
  bottomButton: {
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: 'blue',
    width: 70,
    height: 70,
    borderRadius: 100,
  },
  handler: {
    height: 140,
    width: 140,
    overflow: 'visible',
    marginLeft: -70,
    marginTop: -70,
    alignItems: 'center',
    justifyContent: 'center',
    position: 'absolute',
    zIndex: 10,
  },
});

export default Cropper;
