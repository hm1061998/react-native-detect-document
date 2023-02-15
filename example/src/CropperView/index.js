import React, { forwardRef, useState } from "react";
import { ActivityIndicator, View } from "react-native";
import Cropper from "./Cropper";

const CropperView = forwardRef((props, ref) => {
  const [isReady, setIsReady] = useState(false);
  const [layout, setLayout] = useState(null);
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
})

export default CropperView