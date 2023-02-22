package com.detectdocument

import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;
import android.graphics.Bitmap
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Size
import org.opencv.core.Rect
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc
import org.opencv.imgcodecs.Imgcodecs

import com.detectdocument.extensions.toBase64
import com.detectdocument.utils.ImageUtil


import java.lang.Math


/**
 * This class uses OpenCV to find document corners.
 *
 * @constructor creates document detector
 */
class DocumentDetectorV2() {

  //test processing image
  fun findDocument(image: String): WritableMap {
    val result: WritableMap = WritableNativeMap()
    val photo: Bitmap = ImageUtil().getImageFromFilePath(image.replace("file://", ""))
    val mat = Mat()
    Utils.bitmapToMat(photo, mat)

    // shrink photo to make it easier to find document corners
    val shrunkImageHeight = 500.0
    Imgproc.resize(
      mat, mat, Size(
        shrunkImageHeight * photo.width / photo.height, shrunkImageHeight
      )
    )

    val orig = mat

    val outputImage = Mat()
    val candy = Mat()
    val lines = Mat()
    val gray = Mat()
    val blur = Mat()

    Imgproc.morphologyEx(
      orig,
      outputImage,
      Imgproc.MORPH_CLOSE,
      Mat.ones(Size(9.0, 9.0), CvType.CV_8U),
    )

    Imgproc.cvtColor(outputImage, gray, Imgproc.COLOR_BGR2GRAY)
    // blur image to help remove noise
    Imgproc.GaussianBlur(gray, blur, Size(5.0, 5.0), 0.0)

    Core.copyMakeBorder(blur, blur, 5, 5, 5, 5, Core.BORDER_CONSTANT)
    // detect the document's border using the Canny edge detection algorithm
    Imgproc.Canny(blur, candy, 20.0, 200.0,3)
    Imgproc.dilate(
      candy, candy, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(3.0, 3.0))
    )
    Imgproc.HoughLines(candy, lines, 1.0, 3.14 / 180, 150); // runs the actual detection
    val newLine = Mat.ones(candy.size(), CvType.CV_8U)
    Core.bitwise_not(newLine, newLine)
    // Draw the lines
    for (x in 0 until lines.rows()) {
      val rho = lines.get(x, 0).get(0)
      val theta = lines.get(x, 0).get(1)
      val a = Math.cos(theta)
      val b = Math.sin(theta)
      val x0 = a * rho
      val y0 = b * rho
      val pt1 =
        Point(Math.round(x0 + 1000.0 * -b).toDouble(), Math.round(y0 + 1000.0 * a).toDouble())
      val pt2 =
        Point(Math.round(x0 - 1000.0 * -b).toDouble(), Math.round(y0 - 1000.0 * a).toDouble())
      Imgproc.line(candy, pt1, pt2, Scalar(255.0, 0.0, 0.0), 2, 8)
      Imgproc.line(newLine, pt1, pt2, Scalar(0.0, 255.0, 0.0), 1, 8)

    }

    Imgproc.Canny(newLine, newLine, 50.0, 200.0, 3)
    Imgproc.dilate(
      newLine, newLine, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, Size(5.0, 5.0))
    )


    // approximate outlines using polygons
    var approxContours = findContours(newLine, false)

    val height = candy.height()
    val width = candy.width()
    val MAX_COUNTOUR_AREA = (width - 10) * (height - 10)

    approxContours = approxContours.filter {
      it.height() > 2 && Imgproc.contourArea(it) < MAX_COUNTOUR_AREA && Imgproc.contourArea(it) > 10000
    }


    val new = Mat.ones(candy.size(), CvType.CV_8U)
    Core.bitwise_not(new, new)
    Imgproc.drawContours(new, approxContours, -1, Scalar(0.0, 255.0, 0.0), -1)

//     // get outline of document edges, and outlines of other shapes in photo
    var contours2 = findContours(new, true)
    contours2 = contours2.filter {
      it.height() == 4 && Imgproc.contourArea(it) < MAX_COUNTOUR_AREA && Imgproc.contourArea(it) > 10000
//         && Imgproc.isContourConvex(it)
    }

    val new2 = Mat.ones(candy.size(), CvType.CV_8U)
    Core.bitwise_not(new2, new2)

    val dataList: MutableList<MatOfPoint?> = mutableListOf()
    val contour = contours2.maxByOrNull { Imgproc.contourArea(it) }

    dataList.add(contour)

    Imgproc.drawContours(new2, dataList, -1, Scalar(0.0, 255.0, 0.0), 0)


    val base64OriginImage = convertMatToBase64(orig)
    val base64OBlurImage = convertMatToBase64(blur)
    val base64OCandy = convertMatToBase64(newLine)
    val base64ONewImage = convertMatToBase64(new2)

    result.putString("image", base64OriginImage)
    result.putString("blur", base64OBlurImage)
    result.putString("candy", base64OCandy)
    result.putString("newImage", base64ONewImage)


    return result
  }


  private fun findContours(mat: Mat, check: Boolean): List<MatOfPoint> {
    val contours: MutableList<MatOfPoint> = mutableListOf()
    Imgproc.findContours(
      mat, contours, Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE
    )

    // approximate outlines using polygons
    var approxContours = contours.map {
      val approxContour = MatOfPoint2f()
      val contour2f = MatOfPoint2f(*it.toArray())
      Imgproc.approxPolyDP(
        contour2f,
        approxContour,
        0.04 * Imgproc.arcLength(contour2f, true),
        check,
      )
      MatOfPoint(*approxContour.toArray())
    }

    return approxContours
  }


  private fun convertMatToBase64(mat: Mat): String {
    val bitmap = Bitmap.createBitmap(
      mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888
    )
    Utils.matToBitmap(mat, bitmap)
    return bitmap.toBase64(100)
  }

  /**
   * take a photo with a document, and find the document's corners
   *
   * @param image a photo with a document
   * @return a list with document corners (top left, top right, bottom right, bottom left)
   */
  fun findDocumentCorners(photo: Bitmap): List<Point>? {
    // convert bitmap to OpenCV matrix
    val mat = Mat()
    Utils.bitmapToMat(photo, mat)

    // shrink photo to make it easier to find document corners
    val shrunkImageHeight = 500.0
    Imgproc.resize(
      mat, mat, Size(
        shrunkImageHeight * photo.width / photo.height, shrunkImageHeight
      )
    )

//         find corners for each color channel, then pick the quad with the largest
//         area, and scale point to account for shrinking image before document detection
    val documentCorners: List<Point>? = findCorners(mat)?.toList()?.map {
      Point(
        it.x * photo.height / shrunkImageHeight, it.y * photo.height / shrunkImageHeight
      )
    }
    return documentCorners?.sortedBy { it.y }?.chunked(2)?.map { it.sortedBy { point -> point.x } }
      ?.flatten()
  }

  /**
   * take an image matrix with a document, and find the document's corners
   *
   * @param image a photo with a document in matrix format (only 1 color space)
   * @return a matrix with document corners or null if we can't find corners
   */
  private fun findCorners(image: Mat): MatOfPoint? {
    val orig = image

    val outputImage = Mat()
    val candy = Mat()
    val lines = Mat()
    val gray = Mat()
    val blur = Mat()

    Imgproc.morphologyEx(
      orig,
      outputImage,
      Imgproc.MORPH_CLOSE,
      Mat.ones(Size(9.0, 9.0), CvType.CV_8U),
    )

    Imgproc.cvtColor(outputImage, gray, Imgproc.COLOR_BGR2GRAY)
    // blur image to help remove noise
    Imgproc.GaussianBlur(gray, blur, Size(5.0, 5.0), 0.0)

    Core.copyMakeBorder(blur, blur, 5, 5, 5, 5, Core.BORDER_CONSTANT)
    // detect the document's border using the Canny edge detection algorithm
    Imgproc.Canny(blur, candy, 20.0, 200.0,3)
    Imgproc.dilate(
      candy, candy, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(3.0, 3.0))
    )
    Imgproc.HoughLines(candy, lines, 1.0, 3.14 / 180, 150); // runs the actual detection
    val newLine = Mat.ones(candy.size(), CvType.CV_8U)
    Core.bitwise_not(newLine, newLine)
    // Draw the lines
    for (x in 0 until lines.rows()) {
      val rho = lines.get(x, 0).get(0)
      val theta = lines.get(x, 0).get(1)
      val a = Math.cos(theta)
      val b = Math.sin(theta)
      val x0 = a * rho
      val y0 = b * rho
      val pt1 =
        Point(Math.round(x0 + 1000.0 * -b).toDouble(), Math.round(y0 + 1000.0 * a).toDouble())
      val pt2 =
        Point(Math.round(x0 - 1000.0 * -b).toDouble(), Math.round(y0 - 1000.0 * a).toDouble())
      Imgproc.line(candy, pt1, pt2, Scalar(255.0, 0.0, 0.0), 2, 8)
      Imgproc.line(newLine, pt1, pt2, Scalar(0.0, 255.0, 0.0), 1, 8)

    }

    Imgproc.Canny(newLine, newLine, 50.0, 200.0, 3)
    Imgproc.dilate(
      newLine, newLine, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, Size(5.0, 5.0))
    )

    // get outline of document edges, and outlines of other shapes in photo

    // approximate outlines using polygons
    var approxContours = findContours(newLine, false)

    val height = candy.height()
    val width = candy.width()
    val MAX_COUNTOUR_AREA = (width - 10) * (height - 10)

    approxContours = approxContours.filter {
      it.height() > 2 && Imgproc.contourArea(it) < MAX_COUNTOUR_AREA && Imgproc.contourArea(it) > 10000
    }

    val new = Mat.ones(candy.size(), CvType.CV_8U)
    Core.bitwise_not(new, new)

    Imgproc.drawContours(new, approxContours, -1, Scalar(0.0, 255.0, 0.0), -1)

    // get outline of document edges, and outlines of other shapes in photo
    var contours2 = findContours(new, true)
    contours2 = contours2.filter {
      it.height() == 4 && Imgproc.contourArea(it) < MAX_COUNTOUR_AREA && Imgproc.contourArea(it) > 10000 && Imgproc.isContourConvex(
        it
      )
    }

    val new2 = Mat.ones(candy.size(), CvType.CV_8U)
    Core.bitwise_not(new2, new2)

    val contour = contours2.maxByOrNull { Imgproc.contourArea(it) }

    return contour
  }


}
