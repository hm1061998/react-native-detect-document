import * as React from 'react';
import {
  StyleSheet,
  View,
  Text,
  TouchableHighlight,
  TouchableOpacity,
  Image,
  FlatList,
  Dimensions,
  ScrollView,
  ActivityIndicator,
  Platform,
  NativeModules,
} from 'react-native';
import RNDDM from 'react-native-detect-document';
import MultipleImagePicker from '@baronha/react-native-multiple-image-picker';
import { GestureHandlerRootView } from 'react-native-gesture-handler';
import { Image as CompresImage, uuidv4 } from 'react-native-compressor';

const { detectFile, CropperView, getResultImage, cleanText } = RNDDM;

const nativeMethods = NativeModules.RnDetectDocument;
const { width } = Dimensions.get('window');
export default function App() {
  const [responseImg, setResponseImg] = React.useState(null);
  const [resultCrop, setResultCrop] = React.useState(null);
  const [resultImage, setresultImage] = React.useState(null);
  const [loading, setLoading] = React.useState(false);

  const [scrollEnabled, setscrollEnabled] = React.useState(true);
  const customCrop = React.useRef();

  // console.log('CropperView', nativeMethods);

  const takePicture = async () => {
    try {
      const response = await MultipleImagePicker.openPicker({
        usedCameraButton: true,
        mediaType: 'image',
        singleSelectedMode: true,
        isPreview: false,
      });
      // const base64 = await ImgToBase64.getBase64String(response.path);

      // setResultCrop(image);
      // console.log('data',corners);

      // setLoading(true);
      const filePath = Platform.select({
        ios: response.path,
        android: `file://${response.realPath}`,
      });

      // // setLoading(true);
      // const res = await getResultImage(filePath);
      // // setLoading(false);
      // setresultImage(res);

      const { corners, width, height } = await detectFile(filePath, true);
      setLoading(false);
      const rectangle = {
        topLeft: corners.TOP_LEFT,
        topRight: corners.TOP_RIGHT,
        bottomRight: corners.BOTTOM_RIGHT,
        bottomLeft: corners.BOTTOM_LEFT,
      };

      setResponseImg({ realPath: filePath, width, height, rectangle });

      // console.log({ response });
    } catch (e) {
      console.log('e', e);
    }
  };

  const crop = async () => {
    // console.log('loading');
    // const res = await customCrop.current.crop(100);
    // const res = await RNDDM.resizeImage(responseImg.realPath, { width: 595 });
    const res = await RNDDM.cleanText(responseImg.realPath);
    // let res = await FilterImage.SharpenImage(responseImg.realPath, 1.5);
    // const uri = await CompresImage.compress(responseImg.realPath, {
    //   compressionMethod: 'auto',
    //   quality: 1,
    //   maxWidth: 595,
    // });
    // const res = await FilterImage.SharpenImage(uri, 1.5);
    // const { image, width, height } = await nativeMethods.rotateImage(
    //   responseImg.realPath,
    //   false
    // );
    console.log('end', res);
    // Image.getSize(res.uri, (w, h) => {
    //   // console.log({ w, h });
    // });
    setResultCrop(res.image);
  };

  const updateImage = (res) => {
    // console.log('res', res);
    // setResultCrop(res);
  };

  if (resultCrop) {
    return (
      <View style={{ flex: 1, padding: 40 }}>
        <Image
          style={{ flex: 1, resizeMode: 'contain' }}
          resizeMode="contain"
          // source={{ uri: `data:image/png;base64,${resultCrop}` }}
          source={{ uri: resultCrop }}
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

  // console.log({ responseImg });

  if (responseImg) {
    return (
      <View style={{ flex: 1 }}>
        <FlatList
          style={{ width: '100%', height: '100%' }}
          data={['1']}
          keyExtractor={(item) => item}
          scrollEnabled={scrollEnabled}
          pagingEnabled
          contentContainerStyle={{ backgroundColor: 'blue' }}
          horizontal
          renderItem={() => {
            return (
              <View style={{ width: width, height: '100%' }}>
                <CropperView
                  updateImage={updateImage}
                  rectangleCoordinates={responseImg.rectangle}
                  initialImage={responseImg.realPath}
                  height={responseImg.height}
                  width={responseImg.width}
                  ref={customCrop}
                  overlayColor="#fff"
                  overlayStrokeColor="#fff"
                  handlerColor="#fff"
                  enablePanStrict={true}
                  onHander={(key) => {
                    setscrollEnabled(key !== 'start');
                    // console.log('key', key);
                  }}
                />
              </View>
            );
          }}
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
            <Text>Quay lại</Text>
          </TouchableOpacity>
        </View>
      </View>
    );
  }

  if (resultImage) {
    return (
      <View style={{ flex: 1 }}>
        <ScrollView>
          <Text>Ảnh gốc</Text>
          <Image
            style={{ height: 200, resizeMode: 'contain' }}
            resizeMode="contain"
            source={{ uri: `data:image/png;base64,${resultImage.image}` }}
          />
          <Text>Lọc nhiễu</Text>
          <Image
            style={{ height: 200, resizeMode: 'contain' }}
            resizeMode="contain"
            source={{ uri: `data:image/png;base64,${resultImage.blur}` }}
          />
          <Text>Tìm đường viền</Text>
          <Image
            style={{ height: 200, resizeMode: 'contain' }}
            resizeMode="contain"
            source={{ uri: `data:image/png;base64,${resultImage.candy}` }}
          />

          <Text>Kết quả</Text>
          <Image
            style={{ height: 200, resizeMode: 'contain' }}
            resizeMode="contain"
            source={{ uri: `data:image/png;base64,${resultImage.newImage}` }}
          />
        </ScrollView>
        <TouchableOpacity
          style={{ height: 50, zIndex: 10 }}
          onPress={() => {
            setresultImage(null);
          }}
        >
          <Text>Quay lại</Text>
        </TouchableOpacity>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <TouchableHighlight
        disabled={loading}
        onPress={takePicture}
        underlayColor="gray"
      >
        {loading ? (
          <ActivityIndicator size="large" color="#00ff00" />
        ) : (
          <Text>Pick image</Text>
        )}
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
