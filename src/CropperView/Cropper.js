import React, { useImperativeHandle, forwardRef } from 'react';
import { Dimensions, Image, View, } from 'react-native';
import Svg, { Polygon } from 'react-native-svg';
import Animated, {
  useSharedValue,
  useAnimatedProps,
  useAnimatedGestureHandler,
  useAnimatedStyle,
} from 'react-native-reanimated';
import {
  PanGestureHandler,
  GestureHandlerRootView,
} from 'react-native-gesture-handler';
import { cropper } from 'react-native-detect-document'

const AnimatedPolygon = Animated.createAnimatedComponent(Polygon);
const screenWidth = Dimensions.get('window').width;

const getViewHeight = (width, height, layout) => {
  const imageRatio = width / height;
  let _viewHeight = 0;
  if (height > width) {
    // if user takes the photo in portrait
    _viewHeight = Math.round(screenWidth / imageRatio);
  } else {
    // if user takes the photo in landscape
    _viewHeight = Math.round(screenWidth * imageRatio);
  }
  _viewHeight = Math.min(_viewHeight, layout.height);
  _viewHeight -= 50;


  return _viewHeight;
};

const getViewWidth = (width, height, viewHeight) => {
  const imageRatio = width / height;
  let _viewWidth = 0;
  if (height > width) {
    // if user takes the photo in portrait
    _viewWidth = Math.round(viewHeight * imageRatio) + 50
  } else {
    // if user takes the photo in landscape
    _viewWidth = Math.round(viewHeight / imageRatio) + 50
  }
  _viewWidth -= 50;

  return _viewWidth;
}

const Cropper = forwardRef(
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
      ...props
    },
    ref
  ) => {
    const viewHeight = getViewHeight(width, height, layout);
    const viewWidth = getViewWidth(width, height, viewHeight)

    const imageCoordinatesToViewCoordinates = (corner) => {
      const x = corner.x * (viewWidth / width);
      const y = corner.y * (viewHeight / height);
      return {
        x,
        y,
      };
    };

    const viewCoordinatesToImageCoordinates = (corner) => {
      return {
        x: (corner.x.value / viewWidth) * width,
        y: (corner.y.value / viewHeight) * height,
      };
    };

    const topLeft = {
      x: useSharedValue(
        rectangleCoordinates
          ? imageCoordinatesToViewCoordinates(rectangleCoordinates.topLeft).x
          : 100
      ),
      y: useSharedValue(
        rectangleCoordinates
          ? imageCoordinatesToViewCoordinates(rectangleCoordinates.topLeft).y
          : 100
      ),
    };

    const topRight = {
      x: useSharedValue(
        rectangleCoordinates
          ? imageCoordinatesToViewCoordinates(rectangleCoordinates.topRight).x
          : screenWidth - 100
      ),
      y: useSharedValue(
        rectangleCoordinates
          ? imageCoordinatesToViewCoordinates(rectangleCoordinates.topRight).y
          : 100
      ),
    };

    const bottomLeft = {
      x: useSharedValue(
        rectangleCoordinates
          ? imageCoordinatesToViewCoordinates(rectangleCoordinates.bottomLeft).x
          : 100
      ),
      y: useSharedValue(
        rectangleCoordinates
          ? imageCoordinatesToViewCoordinates(rectangleCoordinates.bottomLeft).y
          : viewHeight - 100
      ),
    };

    const bottomRight = {
      x: useSharedValue(
        rectangleCoordinates
          ? imageCoordinatesToViewCoordinates(rectangleCoordinates.bottomRight)
            .x
          : screenWidth - 100
      ),
      y: useSharedValue(
        rectangleCoordinates
          ? imageCoordinatesToViewCoordinates(rectangleCoordinates.bottomRight)
            .y
          : viewHeight - 100
      ),
    };

    useImperativeHandle(ref, () => ({
      crop: () => {
        const coordinates = {
          topLeft: viewCoordinatesToImageCoordinates(topLeft),
          topRight: viewCoordinatesToImageCoordinates(topRight),
          bottomLeft: viewCoordinatesToImageCoordinates(bottomLeft),
          bottomRight: viewCoordinatesToImageCoordinates(bottomRight),
          height: height,
          width: width,
        };
        return cropper(initialImage, coordinates).then((res) => {
          props.updateImage(res.image, coordinates);
        });
      },
    }));

    const panResponderTopLeft = useAnimatedGestureHandler(
      {
        onStart: (event, ctx) => {
          ctx.translateX = topLeft.x.value;
          ctx.translateY = topLeft.y.value;
        },
        onActive: (event, ctx) => {
          const offsetX = ctx.translateX;
          const offsetY = ctx.translateY;
          const x = offsetX + event.translationX;
          const y = offsetY + event.translationY;

          if (x >= 0 && y >= 0 && x <= viewWidth && y <= viewHeight) {
            topLeft.x.value = ctx.translateX + event.translationX;
            topLeft.y.value = ctx.translateY + event.translationY;
          }
          // console.log('event', event);
        },
        onEnd: (event, ctx) => { },
      },
      []
    );

    const topLeftStyle = useAnimatedStyle(() => {
      return {
        transform: [
          {
            translateX: topLeft.x.value,
          },
          {
            translateY: topLeft.y.value,
          },
        ],
      };
    }, []);

    const panResponderTopRight = useAnimatedGestureHandler(
      {
        onStart: (event, ctx) => {
          ctx.translateX = topRight.x.value;
          ctx.translateY = topRight.y.value;
        },
        onActive: (event, ctx) => {
          const offsetX = ctx.translateX;
          const offsetY = ctx.translateY;
          const x = offsetX + event.translationX;
          const y = offsetY + event.translationY;

          if (x >= 0 && y >= 0 && x <= viewWidth && y <= viewHeight) {
            topRight.x.value = ctx.translateX + event.translationX;
            topRight.y.value = ctx.translateY + event.translationY;
          }
          // console.log('event', event);
        },
        onEnd: (event, ctx) => { },
      },
      []
    );

    const topRightStyle = useAnimatedStyle(() => {
      return {
        transform: [
          {
            translateX: topRight.x.value,
          },
          {
            translateY: topRight.y.value,
          },
        ],
      };
    }, []);

    const panResponderBottomRight = useAnimatedGestureHandler(
      {
        onStart: (event, ctx) => {
          ctx.translateX = bottomRight.x.value;
          ctx.translateY = bottomRight.y.value;
        },
        onActive: (event, ctx) => {
          const offsetX = ctx.translateX;
          const offsetY = ctx.translateY;
          const x = offsetX + event.translationX;
          const y = offsetY + event.translationY;

          if (x >= 0 && y >= 0 && x <= viewWidth && y <= viewHeight) {
            bottomRight.x.value = ctx.translateX + event.translationX;
            bottomRight.y.value = ctx.translateY + event.translationY;
          }
          // console.log('event', event);
        },
        onEnd: (event, ctx) => { },
      },
      []
    );

    const bottomRightStyle = useAnimatedStyle(() => {
      return {
        transform: [
          {
            translateX: bottomRight.x.value,
          },
          {
            translateY: bottomRight.y.value,
          },
        ],
      };
    }, []);

    const panResponderBottomLeft = useAnimatedGestureHandler(
      {
        onStart: (event, ctx) => {
          ctx.translateX = bottomLeft.x.value;
          ctx.translateY = bottomLeft.y.value;
        },
        onActive: (event, ctx) => {
          const offsetX = ctx.translateX;
          const offsetY = ctx.translateY;
          const x = offsetX + event.translationX;
          const y = offsetY + event.translationY;

          if (x >= 0 && y >= 0 && x <= viewWidth && y <= viewHeight) {
            bottomLeft.x.value = ctx.translateX + event.translationX;
            bottomLeft.y.value = ctx.translateY + event.translationY;
          }
          // console.log('event', event);
        },
        onEnd: (event, ctx) => { },
      },
      []
    );

    const bottomLeftStyle = useAnimatedStyle(() => {
      return {
        transform: [
          {
            translateX: bottomLeft.x.value,
          },
          {
            translateY: bottomLeft.y.value,
          },
        ],
      };
    }, []);

    const animatedPointsValues = [topLeft, topRight, bottomRight, bottomLeft];

    const animatedProps = useAnimatedProps(
      () => ({
        points: animatedPointsValues.map((item) => {
          return [item.x.value, item.y.value];
        }),
      }),
      []
    );

    return (
      <GestureHandlerRootView>
        <View
          style={[
            // s(this.props).cropContainer,
            { height: viewHeight, width: viewWidth },
          ]}
        >
          <Image
            style={[
              // s(this.props).image,
              { flex: 1 },
            ]}
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

          <PanGestureHandler onGestureEvent={panResponderTopLeft}>
            <Animated.View style={[s(props).handler, topLeftStyle]}>
              <View style={[s(props).handlerI, { left: -10, top: -10 }]} />
              <View style={[s(props).handlerRound, { left: 31, top: 31 }]} />
            </Animated.View>
          </PanGestureHandler>

          <PanGestureHandler onGestureEvent={panResponderTopRight}>
            <Animated.View style={[s(props).handler, topRightStyle]}>
              <View style={[s(props).handlerI, { left: 10, top: -10 }]} />
              <View style={[s(props).handlerRound, { right: 31, top: 31 }]} />
            </Animated.View>
          </PanGestureHandler>

          <PanGestureHandler onGestureEvent={panResponderBottomLeft}>
            <Animated.View style={[s(props).handler, bottomLeftStyle]}>
              <View style={[s(props).handlerI, { left: -10, top: 10 }]} />
              <View style={[s(props).handlerRound, { left: 31, bottom: 31 }]} />
            </Animated.View>
          </PanGestureHandler>

          <PanGestureHandler onGestureEvent={panResponderBottomRight}>
            <Animated.View style={[s(props).handler, bottomRightStyle]}>
              <View style={[s(props).handlerI, { left: 10, top: 10 }]} />
              <View
                style={[s(props).handlerRound, { right: 31, bottom: 31 }]}
              />
            </Animated.View>
          </PanGestureHandler>
        </View>
      </GestureHandlerRootView>
    );
  }
);

const s = (props) => ({
  handlerI: {
    borderRadius: 0,
    height: 20,
    width: 20,
    backgroundColor: props.handlerColor || 'blue',
  },
  handlerRound: {
    width: 39,
    position: 'absolute',
    height: 39,
    borderRadius: 100,
    backgroundColor: props.handlerColor || 'blue',
  },
  image: {
    width: screenWidth,
    position: 'absolute',
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
  cropContainer: {
    position: 'absolute',
    left: 0,
    width: screenWidth,
    top: 0,
  },
});


export default Cropper