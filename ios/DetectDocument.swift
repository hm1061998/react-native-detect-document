import UIKit
import Vision
import CoreImage
import CoreGraphics
import OpenCV

@objc(DetectDocument)
class DetectDocument: NSObject {

  @objc(multiply:withB:withResolver:withRejecter:)
  func multiply(a: Float, b: Float, resolve:RCTPromiseResolveBlock,reject:RCTPromiseRejectBlock) -> Void {
    resolve(a*b)
  }

  @objc(findDocumentCorners:withB:withResolver:withRejecter:)
  func findDocumentCorners(originalPhotoPath: String, resolve:RCTPromiseResolveBlock,reject:RCTPromiseRejectBlock) -> Void {
    //     Swift code to detect
    //  4 corners of a document in an image using OpenCV: 

    guard let src = cv2.imread("image.jpg") else {
        reject("Error loading image")
        return
    }

    let gray = cv2.cvtColor(src, cv2.COLOR_BGR2GRAY)
    let blur = cv2.GaussianBlur(gray, (7, 7), 0)
    let edges = cv2.Canny(blur, 50, 200, nil)
    let contours = cv2.findContours(edges, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
    let contourPoints = contours[0]

    var points = [CGPoint]()
    for i in 0..<contourPoints.count {
        points.append(CGPoint(x: contourPoints[i][0][0], y: contourPoints[i][0][1]))
    }

    let result = OpenCV.detectDocumentRect(points: points, src: src)
    resolve(result)
  }
}



// Sau đ
// ây là một đoạn mã swift sử dụng OpenCV để xác định và trả về tọa độ các góc của tài liệu trong hình ảnh:

// import cv2
 
// img = cv2.imread('image.jpg')

// gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)

// # Thiết lập thông số canny bề mặt
// edges = cv2.Canny(gray, 100, 200)

// # Tìm contours
// contours, hierarchy = cv2.findContours(edges, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_NONE)

// # Lặp qua tất cả các contours
// for cnt in contours:
//     # Định dạng lại các góc của contours
//     epsilon = 0.01 * cv2.arcLength(cnt, True)
//     approx = cv2.approxPolyDP(cnt, epsilon, True)
 
//     # Tính toán tọa độ của các góc
//     x = approx.ravel()[0]
//     y = approx.ravel()[1]
 
//     # Vẽ các góc
//     cv2.circle(img, (x, y), 5, (0, 0, 255), -1)
 
// # Hiển thị kết quả
// cv2.imshow("Result", img)
// cv2.waitKey(0)





// Using Swift and Open
// CV, you can scan documents using the following code: 

// import UIKit
// import Swift
// import OpenCV

// let documentImage: UIImage
// guard let cvImage = documentImage.toCvMat() else {
//     return
// }

// // Perform OpenCV operations
// let processedImage = OpenCV.processImage(cvImage)

// // Get a UIImage from processed OpenCV image
// let resultImage = UIImage(cvMat: processedImage)