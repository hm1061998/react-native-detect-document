import * as React from 'react';
import {
  StyleSheet,
  View,
  Text,
  TouchableHighlight,
  Dimensions,
  TouchableOpacity,
  Image,
} from 'react-native';
import RNDDM from 'react-native-detect-document';

// import CropperView from 'rn-detect-document/src/ImageCropper';
// import { findDocumentCorrers, CropperView } from '../../src';
// import CropperView from '../../src/ImageCropper/index'
import MultipleImagePicker from '@baronha/react-native-multiple-image-picker';
import ImgToBase64 from 'react-native-image-base64';
import  CropperView  from "./CropperView";

const { findDocumentCorrers } = RNDDM

const windowWidth = Dimensions.get('window').width;
const windowHeight = Dimensions.get('window').height;
export default function App() {
  const [result, setResult] = React.useState();
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
      // const base64 = await ImgToBase64.getBase64String(response.path);
      const { corners, width, height } = await findDocumentCorrers(
        response.realPath
      );

      // console.log('data',corners);
      const rectangle = {
        topLeft: corners.TOP_LEFT,
        topRight: corners.TOP_RIGHT,
        bottomRight: corners.BOTTOM_RIGHT,
        bottomLeft: corners.BOTTOM_LEFT,
      };

      setResponseImg({ realPath: response.realPath, width, height, rectangle });

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

  const updateImage = (res, coordinates) => {
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
          <Text>Quay lại</Text>
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
          initialImage={`file://${responseImg.realPath}`}
          height={responseImg.height}
          width={responseImg.width}
          ref={customCrop}
          overlayColor="rgba(18,190,210, 1)"
          overlayStrokeColor="rgba(20,190,210, 1)"
          handlerColor="rgba(20,150,160, 1)"
          enablePanStrict={false}
        />
        <View style={{ flexDirection: 'row', paddingVertical: 10, height: 300 }}>
          <TouchableOpacity onPress={crop} style={{ flex: 1 }}>
            <Text>CROP IMAGE</Text>
          </TouchableOpacity>
          <TouchableOpacity
            onPress={() => setResponseImg(null)}
            style={{ flex: 1 }}
          >
            <Text>Quay lại</Text>
          </TouchableOpacity>
        </View>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <Text>Result: {result}</Text>

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
