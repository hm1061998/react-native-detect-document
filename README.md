# react-native-detect-document

[![Npm package version](https://img.shields.io/npm/v/react-native-detect-document/latest.svg?style=for-the-badge&logo=npm)](https://www.npmjs.com/package/react-native-detect-document) [![npm dev dependency version](https://img.shields.io/npm/dependency-version/react-native-detect-document/dev/react-native?color=61DAFB&logo=react&style=for-the-badge)](https://github.com/hm1061998/react-native-detect-document/blob/master/package.json)

The react-native library helps you detect document in image for Android and IOS

Thanks library [react-native-document-scanner-plugin](https://github.com/websitebeaver/react-native-document-scanner-plugin) gave me the idea to make this project

![](https://github.com/hm1061998/react-native-detect-document/blob/main/demo.gif)

## Installation

```sh
npm install react-native-detect-document react-native-reanimated react-native-svg react-native-gesture-handler

or

yarn add react-native-detect-document react-native-reanimated react-native-svg react-native-gesture-handler
```

## IOS

```sh
cd ios && pod install
```

## Usage

```js
import * as React from 'react';
import {
  StyleSheet,
  View,
  Text,
  TouchableHighlight,
  TouchableOpacity,
  Image,
  Platform,
} from 'react-native';
import RNDDM from 'react-native-detect-document';
import MultipleImagePicker from '@baronha/react-native-multiple-image-picker';
// import CropperView from "./CropperView";
const { detectFile, CropperView } = RNDDM;

export default function App() {
  const [responseImg, setResponseImg] = React.useState(null);
  const [resultCrop, setResultCrop] = React.useState(null);
  const customCrop = React.useRef();

  // console.log('CropperView', RNDDM);

  const takePicture = async () => {
    try {
      const response = await MultipleImagePicker.openPicker({
        usedCameraButton: true,
        mediaType: 'image',
        singleSelectedMode: true,
        isPreview: false,
      });

      const filePath = Platform.select({
        ios: response.path,
        android: `file://${response.realPath}`,
      });
      const { corners, width, height } = await detectFile(filePath);

      // console.log('data',corners);
      const rectangle = {
        topLeft: corners.TOP_LEFT,
        topRight: corners.TOP_RIGHT,
        bottomRight: corners.BOTTOM_RIGHT,
        bottomLeft: corners.BOTTOM_LEFT,
      };

      setResponseImg({ realPath: filePath, width, height, rectangle });

      // console.log({ corrers});
    } catch (e) {
      console.log('e', e);
    }
  };

  const crop = async () => {
    // console.log('loading');
    await customCrop.current.crop();
    // console.log('end');
  };

  const updateImage = (res) => {
    // console.log('res',res);
    setResultCrop(res);
  };

  if (resultCrop) {
    return (
      <View style={{ flex: 1, padding: 40 }}>
        <Image
          style={{ flex: 1, resizeMode: 'contain' }}
          resizeMode="contain"
          source={{ uri: `data:image/png;base64,${resultCrop}` }}
        />
        <TouchableOpacity
          style={{ height: 50, zIndex: 10 }}
          onPress={() => {
            setResultCrop(null);
            setResponseImg(null);
          }}
        >
          <Text>Back</Text>
        </TouchableOpacity>
      </View>
    );
  }

  // console.log({responseImg});

  if (responseImg) {
    return (
      <View style={{ flex: 1 }}>
        <CropperView
          updateImage={updateImage}
          rectangleCoordinates={responseImg.rectangle}
          initialImage={responseImg.realPath}
          height={responseImg.height}
          width={responseImg.width}
          ref={customCrop}
          overlayColor="rgba(18,190,210, 1)"
          overlayStrokeColor="rgba(20,190,210, 1)"
          handlerColor="rgba(20,150,160, 1)"
          enablePanStrict={false}
        />
        <View
          style={{ flexDirection: 'row', paddingVertical: 10, height: 300 }}
        >
          <TouchableOpacity onPress={crop} style={{ flex: 1 }}>
            <Text>CROP IMAGE</Text>
          </TouchableOpacity>
          <TouchableOpacity
            onPress={() => setResponseImg(null)}
            style={{ flex: 1 }}
          >
            <Text>Back</Text>
          </TouchableOpacity>
        </View>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <TouchableHighlight onPress={takePicture} underlayColor="gray">
        <Text>Pick image</Text>
      </TouchableHighlight>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  box: {
    width: 60,
    height: 60,
    marginVertical: 20,
  },
});
```

## Contributing

See the [contributing guide](CONTRIBUTING.md) to learn how to contribute to the repository and the development workflow.

## License

MIT

---

Made with [create-react-native-library](https://github.com/callstack/react-native-builder-bob)
