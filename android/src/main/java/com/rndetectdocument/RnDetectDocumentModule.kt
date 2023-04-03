package com.rndetectdocument

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.PointF
import android.net.Uri
import com.detectdocument.DocumentDetector
import com.detectdocument.enums.QuadCorner
import com.detectdocument.extensions.move
import com.detectdocument.extensions.saveToFile
import com.detectdocument.models.Quad
import com.detectdocument.utils.ImageUtil
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.WritableMap
import com.facebook.react.bridge.WritableNativeMap
import org.opencv.core.Point

class RnDetectDocumentModule(reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {
    private val cropperOffsetWhenCornersNotFound = 100.0

    override fun getName(): String {
        return NAME
    }

    @ReactMethod
    fun getResultImage(originalPhotoPath: String, promise: Promise) {
        // val photo: Bitmap = ImageUtil().getImageFromFilePath(originalPhotoPath.replace("file://",
        // ""))
        try {
            // load OpenCV
            System.loadLibrary("opencv_java4")
        } catch (exception: Exception) {
            promise.reject("error starting OpenCV: ${exception.message}")
        }
        val result = DocumentDetector().findDocument(originalPhotoPath.replace("file://", ""))

        promise.resolve(result)
    }

    @ReactMethod
    fun rotateImage(originalPhotoPath: String, isClockwise: Boolean?, promise: Promise) {
        val context: Activity? = getCurrentActivity()
        val result: WritableMap = WritableNativeMap()
        val photo: Bitmap =
            ImageUtil().rotate(originalPhotoPath.replace("file://", ""), isClockwise ?: true)
        // val base64 = photo.toBase64(100)
        val imageFile = ImageUtil().createImageFile(context)
        photo.saveToFile(imageFile, 100)
        // val base64 = photo.toBase64(quality ?: 100)
        result.putString("image", Uri.fromFile(imageFile).toString())
        // result.putString("image", base64)
        result.putInt("width", photo.width.toInt())
        result.putInt("height", photo.height.toInt())

        promise.resolve(result)
    }

    @ReactMethod
    fun cleanText(originalPhotoPath: String, promise: Promise) {
        val context: Activity? = getCurrentActivity()
        val result: WritableMap = WritableNativeMap()
        val photo: Bitmap =
            ImageUtil()
                .cleanText(
                    originalPhotoPath.replace("file://", ""),
                )
        // val base64 = photo.toBase64(100)
        val imageFile = ImageUtil().createImageFile(context)
        photo.saveToFile(imageFile, 100)
        // val base64 = photo.toBase64(quality ?: 100)
        result.putString("image", Uri.fromFile(imageFile).toString())
        // result.putString("image", base64)
        result.putInt("width", photo.width.toInt())
        result.putInt("height", photo.height.toInt())

        promise.resolve(result)
    }

    @ReactMethod
    fun resizeImage(originalPhotoPath: String, options: ReadableMap, promise: Promise) {
        val context: Activity? = getCurrentActivity()
        val result: WritableMap = WritableNativeMap()

        val quality: Int = options?.getInt("quality") ?: 100

        val photo: Bitmap =
            ImageUtil()
                .resize(
                    originalPhotoPath.replace("file://", ""),
                    options
                )
        val imageFile = ImageUtil().createImageFile(context)
        photo.saveToFile(imageFile, quality)
        result.putString("uri", Uri.fromFile(imageFile).toString())
       
        promise.resolve(result)
    }

    @ReactMethod
    fun cropImage(
        originalPhotoPath: String,
        points: ReadableMap,
        quality: Int?,
        rotateDeg: Int?,
        promise: Promise
    ) {
        val result: WritableMap = WritableNativeMap()
        val context: Activity? = getCurrentActivity()
        val photo: Bitmap =
            ImageUtil().crop(originalPhotoPath.replace("file://", ""), points, rotateDeg ?: 0)
        val imageFile = ImageUtil().createImageFile(context)
        photo.saveToFile(imageFile, quality ?: 100)
        // val base64 = photo.toBase64(quality ?: 100)
        result.putString("image", Uri.fromFile(imageFile).toString())
        // result.putString("image", base64)

        promise.resolve(result)
    }

    @ReactMethod
    fun detectFile(originalPhotoPath: String, promise: Promise) {
        try {
            // load OpenCV
            System.loadLibrary("opencv_java4")
        } catch (exception: Exception) {
            promise.reject("error starting OpenCV: ${exception.message}")
        }

        val photo: Bitmap =
            ImageUtil().getImageFromFilePath(originalPhotoPath.replace("file://", ""))

        val (topLeft, topRight, bottomLeft, bottomRight) = getDocumentCorners(photo)
        val corners = Quad(topLeft, topRight, bottomRight, bottomLeft)

        val info: WritableMap = WritableNativeMap()
        val points: WritableMap = WritableNativeMap()
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
        val cornerPoints: List<Point>? = DocumentDetector().findDocumentCorners(photo)

        // if cornerPoints is null then default the corners to the photo bounds with a margin

        return cornerPoints
            ?: listOf(
                Point(0.0, 0.0)
                    .move(cropperOffsetWhenCornersNotFound, cropperOffsetWhenCornersNotFound),
                Point(photo.width.toDouble(), 0.0)
                    .move(-cropperOffsetWhenCornersNotFound, cropperOffsetWhenCornersNotFound),
                Point(0.0, photo.height.toDouble())
                    .move(cropperOffsetWhenCornersNotFound, -cropperOffsetWhenCornersNotFound),
                Point(photo.width.toDouble(), photo.height.toDouble())
                    .move(-cropperOffsetWhenCornersNotFound, -cropperOffsetWhenCornersNotFound)
            )

    }

    companion object {
        const val NAME = "RnDetectDocument"
    }
}
