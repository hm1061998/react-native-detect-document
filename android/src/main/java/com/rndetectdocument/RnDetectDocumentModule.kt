package com.rndetectdocument

import android.graphics.Bitmap
import android.graphics.PointF

import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.Promise
import org.opencv.core.Point
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.core.Mat

import com.detectdocument.utils.ImageUtil
import com.detectdocument.models.Quad
import com.detectdocument.DocumentDetector
import com.detectdocument.DocumentDetectorV2
import com.detectdocument.extensions.move
import com.detectdocument.extensions.toBase64
import com.detectdocument.enums.QuadCorner


class RnDetectDocumentModule(reactContext: ReactApplicationContext) :
  ReactContextBaseJavaModule(reactContext) {
  private val cropperOffsetWhenCornersNotFound = 100.0

  override fun getName(): String {
    return NAME
  }


  @ReactMethod
  fun getResultImage(originalPhotoPath: String, promise: Promise) {
    // val photo: Bitmap = ImageUtil().getImageFromFilePath(originalPhotoPath.replace("file://", ""))
    try {
      // load OpenCV
      System.loadLibrary("opencv_java4")
    } catch (exception: Exception) {
      promise.reject(
        "error starting OpenCV: ${exception.message}"
      )
    }
    val result = DocumentDetector2().findDocument(originalPhotoPath.replace("file://", ""))



    promise.resolve(result)
  }

  @ReactMethod
  fun cropper(originalPhotoPath: String, points: ReadableMap, quality: Int, promise: Promise) {
    val photo: Bitmap = ImageUtil().cropv2(originalPhotoPath.replace("file://", ""), points)

    val result: WritableMap = WritableNativeMap()

    val base64 = photo.toBase64(quality ?: 100)

    result.putString("image", base64)

    promise.resolve(result)
  }

  @ReactMethod
  fun findDocumentCorrers(originalPhotoPath: String, promise: Promise) {
    try {
      // load OpenCV
      System.loadLibrary("opencv_java4")
    } catch (exception: Exception) {
      promise.reject(
        "error starting OpenCV: ${exception.message}"
      )
    }

    val photo: Bitmap = ImageUtil().getImageFromFilePath(originalPhotoPath.replace("file://", ""))

    val (topLeft, topRight, bottomLeft, bottomRight) = getDocumentCorners(photo)
    val corners = Quad(topLeft, topRight, bottomRight, bottomLeft)

    val info: WritableMap = WritableNativeMap()
    val points: WritableMap = WritableNativeMap()
    val results: WritableArray = WritableNativeArray()
    for ((quadCorner: QuadCorner, cornerPoint: PointF) in corners.corners) {
      val item: WritableMap = WritableNativeMap()
      item.putInt("x", cornerPoint.x.toInt())
      item.putInt("y", cornerPoint.y.toInt())
      points.putMap(quadCorner.toString(), item)
    }


    info.putInt("width", photo.width.toInt())
    info.putInt("height", photo.height.toInt())
    info.putMap("corners", points)

    promise.resolve(info)
  }

  private fun getDocumentCorners(photo: Bitmap): List<Point> {
    val cornerPoints: List<Point>? = DocumentDetectorV2().findDocumentCorners(photo)

    // if cornerPoints is null then default the corners to the photo bounds with a margin
    return cornerPoints ?: listOf(
      Point(0.0, 0.0).move(
        cropperOffsetWhenCornersNotFound,
        cropperOffsetWhenCornersNotFound
      ),
      Point(photo.width.toDouble(), 0.0).move(
        -cropperOffsetWhenCornersNotFound,
        cropperOffsetWhenCornersNotFound
      ),
      Point(0.0, photo.height.toDouble()).move(
        cropperOffsetWhenCornersNotFound,
        -cropperOffsetWhenCornersNotFound
      ),
      Point(photo.width.toDouble(), photo.height.toDouble()).move(
        -cropperOffsetWhenCornersNotFound,
        -cropperOffsetWhenCornersNotFound
      )
    )
  }

  companion object {
    const val NAME = "RnDetectDocument"
  }
}
