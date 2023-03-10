package com.detectdocument.utils

import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReadableMap
import android.content.ContentResolver
import android.content.Context
import android.media.ExifInterface
import android.graphics.Matrix
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.detectdocument.extensions.distance
import kotlin.math.min
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Size
import org.opencv.core.Core
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc


import java.io.File;
import java.text.SimpleDateFormat
import android.os.Environment
import java.util.Date
import java.util.Locale

import androidx.activity.ComponentActivity
import android.app.Activity;
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

  // fun getImageFromFilePath(filePath: String): Bitmap {
  //   // read image as matrix using OpenCV
  //   val image: Mat = Imgcodecs.imread(filePath)

  //   // convert image to RGB color space since OpenCV reads it using BGR color space
  //   Imgproc.cvtColor(image, image, Imgproc.COLOR_RGB2BGR)

  //   // convert image matrix to bitmap
  //   val bitmap = Bitmap.createBitmap(image.cols(), image.rows(), Bitmap.Config.ARGB_8888)
  //   Utils.matToBitmap(image, bitmap)
  //   return bitmap
  // }

  fun getImageFromFilePath(filePath: String): Bitmap {
    // convert image matrix to bitmap
    val origBitmap = BitmapFactory.decodeFile(filePath)
   
    val exif: ExifInterface = ExifInterface(filePath)    //Since API Level 5
    val exifOrientation:Int = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1)
    val matrix = Matrix()
    if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_90) {
      matrix.postRotate(90.toFloat())
    }
    val bitmap = Bitmap.createBitmap(origBitmap, 0, 0, origBitmap.getWidth(), origBitmap.getHeight(), matrix, true)
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
 
  fun crop(photoFilePath: String, corners: ReadableMap, rotateDeg: Int?): Bitmap {
    // read image with OpenCV
    val bitmap: Bitmap = getImageFromFilePath(photoFilePath.replace("file://", ""))
    val image = Mat()
    Utils.bitmapToMat(bitmap, image)
 
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


    val tLC = Point(topLeftX, topLeftY)
    val tRC = Point(topRightX, topRightY)
    val bRC = Point(bottomRightX, bottomRightY)
    val bLC = Point(bottomLeftX, bottomLeftY)


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

    if(rotateDeg == 90){
      Core.rotate(output, output, Core.ROTATE_90_CLOCKWISE);
    }
    else if(rotateDeg == 180){
      Core.rotate(output, output, Core.ROTATE_180);
    }
    else if(rotateDeg == 270){
      Core.rotate(output, output, Core.ROTATE_90_COUNTERCLOCKWISE);
    }

    // convert output image matrix to bitmap
    val croppedBitmap = Bitmap.createBitmap(
      output.cols(),
      output.rows(),
      Bitmap.Config.ARGB_8888
    )
    Utils.matToBitmap(output, croppedBitmap)

    return croppedBitmap
  }

  fun rotate(photoFilePath: String, isClockwise: Boolean): Bitmap {
    // read image with OpenCV
    // val image = Imgcodecs.imread(photoFilePath.replace("file://", ""))
    val bitmap: Bitmap = getImageFromFilePath(photoFilePath.replace("file://", ""))
    val image = Mat()
    Utils.bitmapToMat(bitmap, image)

    val output = Mat()
    var value = if(isClockwise == true) Core.ROTATE_90_CLOCKWISE else Core.ROTATE_90_COUNTERCLOCKWISE
     
    Core.rotate(image, output, value)

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

  fun createImageFile(context: Activity?): File {
    // use current time to make file name more unique
    val dateTime: String = SimpleDateFormat(
        "yyyyMMdd_HHmmss",
        Locale.US
    ).format(Date())

    val fileNameToSave =  "LAYWERPRO_${dateTime}.jpg"
    var dir : String
    if(context == null){
      dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString()
    }
    else{
      // create file in pictures directory
     dir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString()
    }
   
   val file : File = File(dir, fileNameToSave)
    file.createNewFile()

    return file
    }

    
}
