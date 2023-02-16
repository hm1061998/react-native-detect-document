/* eslint-disable prettier/prettier */
import * as React from 'react';
import { ActivityIndicator, View } from 'react-native';
import Cropper from './Cropper';
import type { CropperViewProps, CropperHandle } from '../types';

const CropperView = React.forwardRef<CropperHandle, CropperViewProps>(
  (props: CropperViewProps, ref) => {
    const [isReady, setIsReady] = React.useState<Boolean>(false);
    const [layout, setLayout] = React.useState<
      | {
          width: number;
          height: number;
        }
      | any
    >(null);
    return (
      <View
        onLayout={(e) => {
          setLayout(e.nativeEvent.layout);
          setIsReady(true);
        }}
        style={{
          flex: 1,
          alignItems: 'center',
          justifyContent: 'center',
          backgroundColor: '#000',
        }}
      >
        {isReady ? (
          <Cropper {...props} ref={ref} layout={layout} />
        ) : (
          <ActivityIndicator size="large" color="#00ff00" />
        )}
      </View>
    );
  }
);

export default CropperView;
