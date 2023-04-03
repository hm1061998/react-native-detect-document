package com.detectdocument

import android.graphics.Bitmap
import com.detectdocument.extensions.calcPoint
import com.detectdocument.extensions.toBase64
import com.detectdocument.utils.ImageUtil
import com.facebook.react.bridge.WritableMap
import com.facebook.react.bridge.WritableNativeMap
import java.lang.Math
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.RotatedRect
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

/**
 * This class uses OpenCV to find document corners.
 *
 * @constructor creates document detector
 */
class DocumentDetector() {
    /**
     * take a photo with a document, and find the document's corners
     *
     * @param image a photo with a document
     * @return a list with document corners (top left, top right, bottom right, bottom left)
     */
    fun findDocumentCorners(photo: Bitmap): List<Point>? {
        // convert bitmap to OpenCV matrix
        val mat = Mat()
        val resized = Mat()
        val output = Mat()
        Utils.bitmapToMat(photo, mat)
        // Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGB2BGR)
        // shrink photo to make it easier to find document corners
        val shrunkImageHeight = 500.0
        Imgproc.resize(
            mat,
            resized,
            Size(shrunkImageHeight * photo.width / photo.height, shrunkImageHeight)
        )

        Imgproc.cvtColor(resized, output, Imgproc.COLOR_BGR2Luv)
        val imageSplitByColorChannel: List<Mat> = mutableListOf()
        Core.split(output, imageSplitByColorChannel)

        var documentCorners: List<Point>? =
            imageSplitByColorChannel
                .mapNotNull { findCorners(it) }
                .maxByOrNull { Imgproc.contourArea(it) }
                ?.toList()
                ?.map {
                    Point(
                        it.x * photo.height / shrunkImageHeight,
                        it.y * photo.height / shrunkImageHeight
                    )
                }

        if (documentCorners.isNullOrEmpty()) {

            documentCorners =
                findCornersVer2(resized)?.toList()?.map {
                    Point(
                        it.x * photo.height / shrunkImageHeight,
                        it.y * photo.height / shrunkImageHeight
                    )
                }

            documentCorners =
                documentCorners
                    ?.sortedBy { it.y }
                    ?.chunked(2)
                    ?.map { it.sortedBy { point -> point.x } }
                    ?.flatten()

            if (documentCorners.isNullOrEmpty()) {
                return documentCorners
            } else {
                val ratio = photo.height.toDouble() / 500.0
                val borderSize = 10.0 * ratio
                val (topLeft, topRight, bottomLeft, bottomRight) = documentCorners

                return listOf(
                    Point(topLeft.x - borderSize, topLeft.y - borderSize)
                        .calcPoint(photo.width.toDouble(), photo.height.toDouble()),
                    Point(topRight.x + borderSize / 2, topRight.y - borderSize)
                        .calcPoint(photo.width.toDouble(), photo.height.toDouble()),
                    Point(bottomLeft.x - borderSize, bottomLeft.y + borderSize / 2)
                        .calcPoint(photo.width.toDouble(), photo.height.toDouble()),
                    Point(bottomRight.x + borderSize / 2, bottomRight.y)
                        .calcPoint(photo.width.toDouble(), photo.height.toDouble())
                )
            }
        }

        return documentCorners
            ?.sortedBy { it.y }
            ?.chunked(2)
            ?.map { it.sortedBy { point -> point.x } }
            ?.flatten()
    }

    /**
     * take an image matrix with a document, and find the document's corners
     *
     * @param image a photo with a document in matrix format (only 1 color space)
     * @return a matrix with document corners or null if we can't find corners
     */
    private fun findCorners(image: Mat): MatOfPoint? {
        val outputImage = Mat()
        // blur image to help remove noise
        Imgproc.GaussianBlur(image, outputImage, Size(5.0, 5.0), 0.0)
        // convert all pixels to either black or white (document should be black after this), but
        // there might be other parts of the photo that turn black
        Imgproc.threshold(
            outputImage,
            outputImage,
            0.0,
            255.0,
            Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU
        )
        // detect the document's border using the Canny edge detection algorithm
        Imgproc.Canny(outputImage, outputImage, 50.0, 200.0)
        // the detect edges might have gaps, so try to close those
        Imgproc.morphologyEx(
            outputImage,
            outputImage,
            Imgproc.MORPH_CLOSE,
            Mat.ones(Size(5.0, 5.0), CvType.CV_8U)
        )
        // get outline of document edges, and outlines of other shapes in photo
        val contours: MutableList<MatOfPoint> = mutableListOf()
        Imgproc.findContours(
            outputImage,
            contours,
            Mat(),
            Imgproc.RETR_LIST,
            Imgproc.CHAIN_APPROX_SIMPLE
        )
        // approximate outlines using polygons
        var approxContours =
            contours.map {
                val approxContour = MatOfPoint2f()
                val contour2f = MatOfPoint2f(*it.toArray())
                Imgproc.approxPolyDP(
                    contour2f,
                    approxContour,
                    0.02 * Imgproc.arcLength(contour2f, true),
                    true
                )
                MatOfPoint(*approxContour.toArray())
            }
        // We now have many polygons, so remove polygons that don't have 4 sides since we
        // know the document has 4 sides. Calculate areas for all remaining polygons, and
        // remove polygons with small areas. We assume that the document takes up a large portion
        // of the photo. Remove polygons that aren't convex since a document can't be convex.
        approxContours =
            approxContours.filter {
                it.height() == 4 && Imgproc.contourArea(it) > 10000 && Imgproc.isContourConvex(it)
            }
        // Once we have all large, convex, 4-sided polygons find and return the 1 with the
        // largest area
        return approxContours.maxByOrNull { Imgproc.contourArea(it) }
    }

    private fun findCornersVer2(image: Mat): MatOfPoint? {
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
        Imgproc.Canny(blur, candy, 20.0, 200.0, 3)

        Imgproc.dilate(
            candy,
            candy,
            Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(3.0, 3.0))
        )
        Imgproc.HoughLines(candy, lines, 1.0, 3.14 / 180, 150)
        // runs the actual detection
        val newLine = Mat.ones(candy.size(), CvType.CV_8U)
        // Core.bitwise_not(newLine, newLine)
        // Draw the lines
        for (x in 0 until lines.rows()) {
            val rho = lines.get(x, 0).get(0)
            val theta = lines.get(x, 0).get(1)
            val a = Math.cos(theta)
            val b = Math.sin(theta)
            val x0 = a * rho
            val y0 = b * rho
            val pt1 =
                Point(
                    Math.round(x0 + 1000.0 * -b).toDouble(),
                    Math.round(y0 + 1000.0 * a).toDouble()
                )
            val pt2 =
                Point(
                    Math.round(x0 - 1000.0 * -b).toDouble(),
                    Math.round(y0 - 1000.0 * a).toDouble()
                )
            Imgproc.line(newLine, pt1, pt2, Scalar(255.0, 0.0, 0.0), 1, 8, 0)
        }

        Imgproc.Canny(newLine, newLine, 50.0, 200.0, 3)
        Imgproc.dilate(
            newLine,
            newLine,
            Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, Size(5.0, 5.0))
        )

        // approximate outlines using polygons
        var approxContours = findContours(newLine, false)

        val height = candy.height()
        val width = candy.width()
        val MAX_COUNTOUR_AREA = (width - 10) * (height - 10)

        approxContours =
            approxContours.filter {
                it.height() > 2 &&
                    Imgproc.contourArea(it) < MAX_COUNTOUR_AREA &&
                    Imgproc.contourArea(it) > 10000
            }

        val new = Mat.ones(candy.size(), CvType.CV_8U)
        Core.bitwise_not(new, new)
        Imgproc.drawContours(new, approxContours, -1, Scalar(0.0, 255.0, 0.0), -1)

        // get outline of document edges, and outlines of other shapes in photo
        var contours2 = findContours(new, true)
        contours2 =
            contours2.filter {
                it.height() > 3 &&
                    Imgproc.contourArea(it) < MAX_COUNTOUR_AREA &&
                    Imgproc.contourArea(it) > 10000
            }

        val new2 = Mat.ones(candy.size(), CvType.CV_8U)
        Core.bitwise_not(new2, new2)

        val dataList: MutableList<MatOfPoint?> = mutableListOf()
        val contour = contours2.maxByOrNull { Imgproc.contourArea(it) }
        dataList.add(contour)
        //   Imgproc.drawContours(new2, dataList, -1, Scalar(0.0, 255.0, 0.0), 0)
        //   Tạo hình chữ nhật

        if (contour != null) {
            val dst = MatOfPoint2f()
            contour?.convertTo(dst, CvType.CV_32F)

            val minRect: RotatedRect
            minRect = Imgproc.minAreaRect(dst)
            val box: Array<Point> = arrayOf<Point>(Point(), Point(), Point(), Point())
            minRect.points(box)
            for (j in box.indices) {
                Imgproc.line(
                    new2,
                    box.get(j),
                    box.get((j + 1) % 4),
                    Scalar(0.0, 0.0, 255.0),
                    1,
                    8,
                    0
                )
            }

            Imgproc.Canny(new2, new2, 50.0, 200.0, 3)
            Imgproc.dilate(
                new2,
                new2,
                Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, Size(5.0, 5.0))
            )

            var contours3 = findContours(new2, true)
            contours3 =
                contours3.filter {
                    it.height() == 4 &&
                        Imgproc.contourArea(it) < MAX_COUNTOUR_AREA &&
                        Imgproc.contourArea(it) > 10000
                }

            val contour4 = contours3.maxByOrNull { Imgproc.contourArea(it) }

            return contour4
        }

        return contour
    }

    // private fun findCornersVer2(image: Mat): MatOfPoint? {
    //   val orig = image

    //   val outputImage = Mat()
    //   val candy = Mat()
    //   val lines = Mat()
    //   val gray = Mat()
    //   val blur = Mat()

    //   Imgproc.morphologyEx(
    //     orig,
    //     outputImage,
    //     Imgproc.MORPH_CLOSE,
    //     Mat.ones(Size(9.0, 9.0), CvType.CV_8U),
    //   )

    //   Imgproc.cvtColor(outputImage, gray, Imgproc.COLOR_BGR2GRAY)
    //   // blur image to help remove noise
    //   Imgproc.GaussianBlur(gray, blur, Size(5.0, 5.0), 0.0)

    //   Core.copyMakeBorder(blur, blur, 5, 5, 5, 5, Core.BORDER_CONSTANT)
    //   // detect the document's border using the Canny edge detection algorithm
    //   Imgproc.Canny(blur, candy, 20.0, 200.0,3)
    //   Imgproc.dilate(
    //     candy, candy, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(3.0, 3.0))
    //   )
    //   Imgproc.HoughLines(candy, lines, 1.0, 3.14 / 180, 150); // runs the actual detection
    //   val newLine = Mat.ones(candy.size(), CvType.CV_8U)

    //   // Draw the lines
    //   for (x in 0 until lines.rows()) {
    //     val rho = lines.get(x, 0).get(0)
    //     val theta = lines.get(x, 0).get(1)
    //     val a = Math.cos(theta)
    //     val b = Math.sin(theta)
    //     val x0 = a * rho
    //     val y0 = b * rho
    //     val pt1 =
    //       Point(Math.round(x0 + 1000.0 * -b).toDouble(), Math.round(y0 + 1000.0 * a).toDouble())
    //     val pt2 =
    //       Point(Math.round(x0 - 1000.0 * -b).toDouble(), Math.round(y0 - 1000.0 * a).toDouble())

    //     Imgproc.line(newLine, pt1, pt2, Scalar(255.0, 0.0, 0.0), 1, 8)

    //   }

    //   Imgproc.Canny(newLine, newLine, 50.0, 200.0, 3)
    //   Imgproc.dilate(
    //     newLine, newLine, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, Size(5.0, 5.0))
    //   )

    //   // get outline of document edges, and outlines of other shapes in photo

    //   // approximate outlines using polygons
    //   var approxContours = findContours(newLine, false)

    //   val height = candy.height()
    //   val width = candy.width()
    //   val MAX_COUNTOUR_AREA = (width - 10) * (height - 10)

    //   approxContours = approxContours.filter {
    //     it.height() > 2 && Imgproc.contourArea(it) < MAX_COUNTOUR_AREA && Imgproc.contourArea(it)
    // > 10000
    //   }

    //   val new = Mat.ones(candy.size(), CvType.CV_8U)
    //   Core.bitwise_not(new, new)

    //   Imgproc.drawContours(new, approxContours, -1, Scalar(0.0, 255.0, 0.0), -1)

    //   // get outline of document edges, and outlines of other shapes in photo
    //   var contours2 = findContours(new, true)
    //   contours2 = contours2.filter {
    //     it.height() == 4 && Imgproc.contourArea(it) < MAX_COUNTOUR_AREA &&
    // Imgproc.contourArea(it) > 10000 && Imgproc.isContourConvex(
    //       it
    //     )
    //   }

    //   val contour = contours2.maxByOrNull { Imgproc.contourArea(it) }

    //   return contour
    // }

    private fun findContours(mat: Mat, check: Boolean): List<MatOfPoint> {
        val contours: MutableList<MatOfPoint> = mutableListOf()
        Imgproc.findContours(mat, contours, Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE)

        // approximate outlines using polygons
        var approxContours =
            contours.map {
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
        val bitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(mat, bitmap)
        return bitmap.toBase64(100)
    }

    //  //test processing image
    fun findDocument(image: String): WritableMap {
        val result: WritableMap = WritableNativeMap()
        val photo: Bitmap = ImageUtil().getImageFromFilePath(image.replace("file://", ""))
        val mat = Mat()
        val resized = Mat()
        Utils.bitmapToMat(photo, mat)

        // shrink photo to make it easier to find document corners
        val shrunkImageHeight = 500.0
        Imgproc.resize(
            mat,
            resized,
            Size(shrunkImageHeight * photo.width / photo.height, shrunkImageHeight)
        )

        val orig = resized

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
        Imgproc.Canny(blur, candy, 20.0, 200.0, 3)
        Imgproc.dilate(
            candy,
            candy,
            Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(3.0, 3.0))
        )
        Imgproc.HoughLines(candy, lines, 1.0, 3.14 / 180, 150)
        // runs the actual detection
        val newLine = Mat.ones(candy.size(), CvType.CV_8U)
        // Core.bitwise_not(newLine, newLine)
        // Draw the lines
        for (x in 0 until lines.rows()) {
            val rho = lines.get(x, 0).get(0)
            val theta = lines.get(x, 0).get(1)
            val a = Math.cos(theta)
            val b = Math.sin(theta)
            val x0 = a * rho
            val y0 = b * rho
            val pt1 =
                Point(
                    Math.round(x0 + 1000.0 * -b).toDouble(),
                    Math.round(y0 + 1000.0 * a).toDouble()
                )
            val pt2 =
                Point(
                    Math.round(x0 - 1000.0 * -b).toDouble(),
                    Math.round(y0 - 1000.0 * a).toDouble()
                )
            Imgproc.line(newLine, pt1, pt2, Scalar(255.0, 0.0, 0.0), 1, 8)
        }

        Imgproc.Canny(newLine, newLine, 50.0, 200.0, 3)
        Imgproc.dilate(
            newLine,
            newLine,
            Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, Size(5.0, 5.0))
        )

        // approximate outlines using polygons
        var approxContours = findContours(newLine, false)

        val height = candy.height()
        val width = candy.width()
        val MAX_COUNTOUR_AREA = (width - 10) * (height - 10)

        approxContours =
            approxContours.filter {
                it.height() > 2 &&
                    Imgproc.contourArea(it) < MAX_COUNTOUR_AREA &&
                    Imgproc.contourArea(it) > 10000
            }

        val new = Mat.ones(candy.size(), CvType.CV_8U)
        Core.bitwise_not(new, new)
        Imgproc.drawContours(new, approxContours, -1, Scalar(0.0, 255.0, 0.0), -1)

        // get outline of document edges, and outlines of other shapes in photo
        var contours2 = findContours(new, true)
        contours2 =
            contours2.filter {
                it.height() == 4 &&
                    Imgproc.contourArea(it) < MAX_COUNTOUR_AREA &&
                    Imgproc.contourArea(it) > 10000
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
        val base64ONewImage = convertMatToBase64(resized)

        result.putString("image", base64OriginImage)
        result.putString("blur", base64OBlurImage)
        result.putString("candy", base64OCandy)
        result.putString("newImage", base64ONewImage)

        return result
    }

    // test processing image
    // fun findDocument(image: String): WritableMap {
    //   val result: WritableMap = WritableNativeMap()
    //   val photo: Bitmap = ImageUtil().getImageFromFilePath(image.replace("file://", ""))
    //   val mat = Mat()
    //   Utils.bitmapToMat(photo, mat)

    //   // shrink photo to make it easier to find document corners
    //   val shrunkImageHeight = 500.0
    //   Imgproc.resize(
    //     mat, mat, Size(
    //       shrunkImageHeight * photo.width / photo.height, shrunkImageHeight
    //     )
    //   )

    //   val inputImage = mat

    //   val outputImage = Mat()
    //      // Chuyển đổi không gian màu từ BGR sang HSV
    //   val hsvImage = Mat()
    //   Imgproc.cvtColor(inputImage, hsvImage, Imgproc.COLOR_BGR2HSV)

    //   // Phân ngưỡng ảnh để làm nổi bật các đối tượng có màu tương tự với tài liệu
    //   val lowerColor = Scalar(0.0, 0.0, 0.0)
    //   val upperColor = Scalar(255.0, 255.0, 150.0)
    //   val mask = Mat()
    //   Core.inRange(hsvImage, lowerColor, upperColor, mask)

    //   // Loại bỏ các đối tượng nhỏ và không cần thiết bằng xử lý hình thái học
    //   val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(5.0, 5.0))
    //   val opening = Mat()
    //   Imgproc.morphologyEx(mask, opening, Imgproc.MORPH_OPEN, kernel, Point(-1.0,-1.0), 1)

    //   // Tìm kiếm đường viền của các đối tượng trong ảnh
    //   val contours: MutableList<MatOfPoint> = mutableListOf()
    //   val hierarchy = Mat()
    //   Imgproc.findContours(opening, contours, hierarchy, Imgproc.RETR_EXTERNAL,
    // Imgproc.CHAIN_APPROX_SIMPLE)

    //   // Vẽ đường viền vào ảnh ban đầu
    //    Imgproc.drawContours(inputImage, contours, -1, Scalar(0.0, 255.0, 0.0), -1)

    //   val base64OriginImage = convertMatToBase64(inputImage)
    //   val base64OBlurImage = convertMatToBase64(hsvImage)
    //   val base64OCandy = convertMatToBase64(opening)
    //   val base64ONewImage = convertMatToBase64(inputImage)

    //   result.putString("image", base64OriginImage)
    //   result.putString("blur", base64OBlurImage)
    //   result.putString("candy", base64OCandy)
    //   result.putString("newImage", base64ONewImage)

    //   return result
    // }

}
