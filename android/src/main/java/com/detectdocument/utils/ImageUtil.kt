package com.detectdocument.utils

import com.facebook.react.bridge.ReadableMap
import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.detectdocument.extensions.distance
import com.detectdocument.extensions.toBase64
import com.detectdocument.extensions.toOpenCVPoint
import com.detectdocument.models.Quad
import kotlin.math.min
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Size
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc

/**
 * This class contains helper functions for processing images
 *
 * @constructor creates image util
 */
class ImageUtil {
    /**
     * get bitmap image from file path
     *
     * @param filePath image is saved here
     * @return image bitmap
     */
   
    fun getImageFromFilePath(filePath: String): Bitmap {
        // read image as matrix using OpenCV
        val image: Mat = Imgcodecs.imread(filePath)

        // convert image to RGB color space since OpenCV reads it using BGR color space
        Imgproc.cvtColor(image, image, Imgproc.COLOR_RGB2BGR)

        // convert image matrix to bitmap
        val bitmap = Bitmap.createBitmap(image.cols(), image.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(image, bitmap)
        return bitmap
    }

    /**
     * take a photo with a document, crop everything out but document, and force it to display
     * as a rectangle
     *
     * @param photoFilePath original image is saved here
     * @param corners the 4 document corners
     * @return bitmap with cropped and warped document
     */
    fun crop(photoFilePath: String, corners: Quad): Bitmap {
        // read image with OpenCV
        val image = Imgcodecs.imread(photoFilePath)

        // convert image to RGB color space since OpenCV reads it using BGR color space
        Imgproc.cvtColor(image, image, Imgproc.COLOR_RGB2BGR)

        // convert top left, top right, bottom right, and bottom left document corners from
        // Android points to OpenCV points
        val tLC = corners.topLeftCorner.toOpenCVPoint()
        val tRC = corners.topRightCorner.toOpenCVPoint()
        val bRC = corners.bottomRightCorner.toOpenCVPoint()
        val bLC = corners.bottomLeftCorner.toOpenCVPoint()

        // Calculate the document edge distances. The user might take a skewed photo of the
        // document, so the top left corner to top right corner distance might not be the same
        // as the bottom left to bottom right corner. We could take an average of the 2, but
        // this takes the smaller of the 2. It does the same for height.
        val width = min(tLC.distance(tRC), bLC.distance(bRC))
        val height = min(tLC.distance(bLC), tRC.distance(bRC))

        // create empty image matrix with cropped and warped document width and height
        val croppedImage = MatOfPoint2f(
            Point(0.0, 0.0),
            Point(width, 0.0),
            Point(width, height),
            Point(0.0, height),
        )

        // This crops the document out of the rest of the photo. Since the user might take a
        // skewed photo instead of a straight on photo, the document might be rotated and
        // skewed. This corrects that problem. output is an image matrix that contains the
        // corrected image after this fix.
        val output = Mat()
        Imgproc.warpPerspective(
            image,
            output,
            Imgproc.getPerspectiveTransform(
                MatOfPoint2f(tLC, tRC, bRC, bLC),
                croppedImage
            ),
            Size(width, height)
        )

        // convert output image matrix to bitmap
        val croppedBitmap = Bitmap.createBitmap(
            output.cols(),
            output.rows(),
            Bitmap.Config.ARGB_8888
        )
        Utils.matToBitmap(output, croppedBitmap)

        return croppedBitmap
    }

    fun cropv2(photoFilePath: String, corners: ReadableMap): Bitmap {
        // read image with OpenCV
        val image = Imgcodecs.imread(photoFilePath.replace("file://", ""))

        // convert image to RGB color space since OpenCV reads it using BGR color space
        Imgproc.cvtColor(image, image, Imgproc.COLOR_RGB2BGR)

        // convert top left, top right, bottom right, and bottom left document corners from
        // Android points to OpenCV points

        val topLeftX: Double = corners?.getMap("topLeft")?.getDouble("x") ?: 0.0
        val topLeftY: Double = corners?.getMap("topLeft")?.getDouble("y") ?: 0.0
        val topRightX: Double = corners?.getMap("topRight")?.getDouble("x") ?: 0.0
        val topRightY: Double = corners?.getMap("topRight")?.getDouble("y") ?: 0.0
        val bottomRightX: Double = corners?.getMap("bottomRight")?.getDouble("x") ?: 0.0
        val bottomRightY: Double = corners?.getMap("bottomRight")?.getDouble("y") ?: 0.0
        val bottomLeftX: Double = corners?.getMap("bottomLeft")?.getDouble("x") ?: 0.0
        val bottomLeftY: Double = corners?.getMap("bottomLeft")?.getDouble("y") ?: 0.0


        val tLC = Point(topLeftX , topLeftY ) 
        val tRC = Point(topRightX, topRightY )
        val bRC = Point(bottomRightX ,bottomRightY )
        val bLC = Point(bottomLeftX , bottomLeftY )


        // Calculate the document edge distances. The user might take a skewed photo of the
        // document, so the top left corner to top right corner distance might not be the same
        // as the bottom left to bottom right corner. We could take an average of the 2, but
        // this takes the smaller of the 2. It does the same for height.
        val width = min(tLC.distance(tRC), bLC.distance(bRC))
        val height = min(tLC.distance(bLC), tRC.distance(bRC))

        // create empty image matrix with cropped and warped document width and height
        val croppedImage = MatOfPoint2f(
            Point(0.0, 0.0),
            Point(width, 0.0),
            Point(width, height),
            Point(0.0, height),
        )

        // This crops the document out of the rest of the photo. Since the user might take a
        // skewed photo instead of a straight on photo, the document might be rotated and
        // skewed. This corrects that problem. output is an image matrix that contains the
        // corrected image after this fix.
        val output = Mat()
        Imgproc.warpPerspective(
            image,
            output,
            Imgproc.getPerspectiveTransform(
                MatOfPoint2f(tLC, tRC, bRC, bLC),
                croppedImage
            ),
            Size(width, height)
        )

        // convert output image matrix to bitmap
        val croppedBitmap = Bitmap.createBitmap(
            output.cols(),
            output.rows(),
            Bitmap.Config.ARGB_8888
        )
        Utils.matToBitmap(output, croppedBitmap)

        return croppedBitmap
    }

    /**
     * get bitmap image from file uri
     *
     * @param fileUriString image is saved here and starts with file:///
     * @return bitmap image
     */
    fun readBitmapFromFileUriString(
        fileUriString: String,
        contentResolver: ContentResolver
    ): Bitmap {
        return BitmapFactory.decodeStream(
            contentResolver.openInputStream(Uri.parse(fileUriString))
        )
    }
}