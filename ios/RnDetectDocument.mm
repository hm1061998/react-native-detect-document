#import "RnDetectDocument.h"
#import <math.h>
#import <React/RCTLog.h>

@implementation RnDetectDocument

- (dispatch_queue_t)methodQueue
{
  return dispatch_get_main_queue();
}

int shrunkImageHeight = 500;
int cropperOffsetWhenCornersNotFound = 100;

RCT_EXPORT_MODULE()

RCT_EXPORT_METHOD(rotateImage:(NSURL *)originalPhotoPath
                  isClockwise:(BOOL)isClockwise
                  resolvePromise:(RCTPromiseResolveBlock)resolve
                  rejecter: (RCTPromiseRejectBlock)reject )
{
  UIImage* image = [self pathToUIImage:originalPhotoPath];
  cv::Mat orig = [self convertUIImageToCVMat:image];
  cv::Mat output;
  auto value = isClockwise ? cv::ROTATE_90_CLOCKWISE: cv::ROTATE_90_COUNTERCLOCKWISE;
  cv::rotate(orig, output, value);

  UIImage *newImage = [self convertCVMatToUIImage:output];
  
  NSString * cleaned = [self convertUIImageToFile:newImage quality:100];
  NSDictionary *info = @{
                @"image":cleaned,
                @"width":@(newImage.size.width),
                @"height":@(newImage.size.height),
              };
  resolve(info);
}

RCT_EXPORT_METHOD(cropImage:(NSURL *)originalPhotoPath
                  points:(NSDictionary *)points
                  quality:(int)quality 
                  rotateDeg:(int)rotateDeg
                  resolvePromise:(RCTPromiseResolveBlock)resolve
                  rejecter: (RCTPromiseRejectBlock)reject )
{

  UIImage* image = [self pathToUIImage:originalPhotoPath];
  cv::Mat orig = [self convertUIImageToCVMat:image];

  double topLeftX = [points[@"topLeft"][@"x"] doubleValue];
  double topLeftY  = [points[@"topLeft"][@"y"] doubleValue];
  double topRightX = [points[@"topRight"][@"x"] doubleValue];
  double topRightY  = [points[@"topRight"][@"y"] doubleValue];
  double bottomRightX = [points[@"bottomRight"][@"x"] doubleValue];
  double bottomRightY = [points[@"bottomRight"][@"y"] doubleValue];
  double bottomLeftX  = [points[@"bottomLeft"][@"x"] doubleValue];
  double bottomLeftY = [points[@"bottomLeft"][@"y"] doubleValue];

  cv::Point tLC = cv::Point(topLeftX, topLeftY);
  cv::Point tRC = cv::Point(topRightX, topRightY);
  cv::Point bRC = cv::Point(bottomRightX, bottomRightY);
  cv::Point bLC = cv::Point(bottomLeftX, bottomLeftY);

  double width = fmin([self _distance:tLC p2:tRC], [self _distance:bLC p2:bRC]);
  double height = fmin([self _distance:tLC p2:bLC], [self _distance:tRC p2:bRC]);

  // create empty image matrix with cropped and warped document width and height
  cv::Point2f croppedImage[] = {
    cv::Point(0, 0),
    cv::Point(width, 0),
    cv::Point(width, height),
    cv::Point(0, height)
  };
   
  cv::Point2f src[] = { tLC, tRC, bRC, bLC };
    
  cv::Mat output ;
  cv::warpPerspective(
    orig,
    output,
    cv::getPerspectiveTransform(
      src,
      croppedImage
    ),
    cv::Size(width, height)
  );

  if(rotateDeg == 90){
    cv::rotate(output, output, cv::ROTATE_90_CLOCKWISE);
  }
  else if(rotateDeg == 180){
    cv::rotate(output, output, cv::ROTATE_180);
  }
  else if(rotateDeg == 270){
    cv::rotate(output, output, cv::ROTATE_90_COUNTERCLOCKWISE);
  }

  cv::Mat gray ;
  cv::Mat binary ;

  cv::cvtColor(output, gray, cv::COLOR_BGR2GRAY);
  cv::threshold(gray, binary, 128, 255, cv::THRESH_BINARY);

  UIImage *newImage = [self convertCVMatToUIImage:output];

  NSString * cleaned = [self convertUIImageToFile:newImage quality:quality];
  NSString * grayfile = [self convertUIImageToFile:[self convertCVMatToUIImage:gray] quality:quality];
  NSString * binaryfile = [self convertUIImageToFile:[self convertCVMatToUIImage:binary] quality:quality];

  NSDictionary *info = @{
                @"image":cleaned,
                @"grayfile":grayfile,
                @"binaryfile":binaryfile,
              };
  resolve(info);
}


RCT_EXPORT_METHOD(getResultImage:(NSURL *)filePath
                resolvePromise:(RCTPromiseResolveBlock)resolve
                rejecter: (RCTPromiseRejectBlock)reject)
{
   UIImage* image = [self pathToUIImage:filePath];
  cv::Mat orig = [self convertUIImageToCVMat:image];

  cv::Mat outputImage ;
  cv::Mat candy ;
  // cv::Mat lines ;
  cv::Mat gray ;
  cv::Mat blur ;

  cv::resize(
    orig,
    outputImage,
    cv::Size(
      image.size.width / image.size.height * shrunkImageHeight,
      shrunkImageHeight)
    );

  cv::morphologyEx(
    outputImage,
    outputImage,
    cv::MORPH_CLOSE,
    cv::Mat::ones(cv::Size(9, 9),CV_8U)
  );

  cv::cvtColor(outputImage, gray, cv::COLOR_BGR2GRAY);
  cv::GaussianBlur(gray, blur, cv::Size(5, 5), 0);
  cv::copyMakeBorder(blur, blur, 5, 5, 5, 5, cv::BORDER_CONSTANT);

  cv::Canny(blur, candy, 50, 200, 3);
  cv::dilate(
    candy,
    candy,
    cv::getStructuringElement(cv::MORPH_ELLIPSE, cv::Size(3, 3))
  );

  std::vector<cv::Vec2f> lines;
  cv::HoughLines(candy, lines, 1, 3.14 / 180, 150); // runs the actual detection
  cv::Mat newLine = cv::Mat::ones(cv::Size(candy.cols, candy.rows), CV_8U);

  for( size_t i = 0; i < lines.size(); i++ ) {
    float rho = lines[i][0];
    float theta = lines[i][1];
    double a = cos(theta), b = sin(theta);
    double x0 = a*rho, y0 = b*rho;
    cv::Point pt1(cvRound(x0 + 1000*(-b)), cvRound(y0 + 1000*(a)));
    cv::Point pt2(cvRound(x0 - 1000*(-b)), cvRound(y0 - 1000*(a)));
    cv::line(newLine, pt1, pt2, cv::Scalar(255,0,0), 1, 8);
  }

  cv::Canny(newLine, newLine, 50, 200, 3);
  cv::dilate(
    newLine,
    newLine,
    cv::getStructuringElement(cv::MORPH_ELLIPSE, cv::Size(3, 3))
  );

  std::vector<std::vector<cv::Point> > approxContours = [self findContours:newLine check: false] ;

  float height = candy.cols;
  float width = candy.rows;
  double MAX_COUNTOUR_AREA = (width - 10) * (height - 10);
    
  std::vector<std::vector<cv::Point> > approxContoursFilter;

  for (size_t i = 0; i < approxContours.size(); i++){
      if(
        approxContours[i].size() > 2
        && cv::contourArea(approxContours[i]) < MAX_COUNTOUR_AREA
        && cv::contourArea(approxContours[i]) > 10000){
          approxContoursFilter.push_back(approxContours[i]);
      }
    }

  // cv::Mat newMat = cv::Mat::ones(cv::Size(candy.cols, candy.rows), CV_8U);
  // cv::bitwise_not(newMat, newMat);

  // cv::drawContours(newMat, approxContours, -1, cv::Scalar(0.0, 255.0, 0.0), -1);

  std::vector<std::vector<cv::Point> > approxContoursFinal = [self findContours:newLine check: true] ;
  std::vector<std::vector<cv::Point> > approxContoursFinalFilter;

  std::vector<std::vector<cv::Point> > dataList;
  std::vector<cv::Point> largestArea;
  double maxArea = 0;
   for (size_t i = 0; i < approxContoursFinal.size(); i++){
      if(
        approxContoursFinal[i].size() == 4
        && cv::contourArea(approxContoursFinal[i]) < MAX_COUNTOUR_AREA
        && cv::contourArea(approxContoursFinal[i]) > 10000
        && cv::isContourConvex(approxContoursFinal[i])
        && cv::contourArea(approxContoursFinal[i]) > maxArea){
           largestArea = approxContoursFinal[i];
           maxArea = cv::contourArea(approxContoursFinal[i]);
           
      }
    }

  dataList.push_back(largestArea);
  cv::Mat new2 = cv::Mat::ones(cv::Size(candy.cols, candy.rows), CV_8U);
  cv::bitwise_not(new2, new2);

  cv::drawContours(new2, dataList, -1, cv::Scalar(0.0, 255.0, 0.0), 0);
   NSString *base64OriginImage = [self convertMatToBase64:orig];
   NSString *base64OBlurImage = [self convertMatToBase64:blur];
   NSString *base64OCandy = [self convertMatToBase64:newLine];
   NSString *base64ONewImage = [self convertMatToBase64:new2];
  


      NSDictionary *info = @{
                  @"image":base64OriginImage,
                  @"blur":base64OBlurImage,
                  @"candy":base64OCandy,
                  @"newImage":base64ONewImage,
                  };

  

  resolve(info);
}

RCT_EXPORT_METHOD(detectFile:(NSURL *)filePath
                resolvePromise:(RCTPromiseResolveBlock)resolve
                rejecter: (RCTPromiseRejectBlock)reject )
{
  
  UIImage* image = [self pathToUIImage:filePath];
  cv::Mat orig = [self convertUIImageToCVMat:image];

  cv::Mat outputImage ;
  cv::Mat candy ;
  // cv::Mat lines ;
  cv::Mat gray ;
  cv::Mat blur ;

  cv::resize(
    orig,
    outputImage,
    cv::Size(
      image.size.width / image.size.height * shrunkImageHeight,
      shrunkImageHeight)
    );

  cv::morphologyEx(
    outputImage,
    outputImage,
    cv::MORPH_CLOSE,
    cv::Mat::ones(cv::Size(9, 9),CV_8U)
  );

  cv::cvtColor(outputImage, gray, cv::COLOR_BGR2GRAY);
  cv::GaussianBlur(gray, blur, cv::Size(5, 5), 0);
  cv::copyMakeBorder(blur, blur, 5, 5, 5, 5, cv::BORDER_CONSTANT);

  cv::Canny(blur, candy, 20, 200, 3);
  cv::dilate(
    candy,
    candy,
    cv::getStructuringElement(cv::MORPH_RECT, cv::Size(3, 3))
  );

  std::vector<cv::Vec2f> lines;
  cv::HoughLines(candy, lines, 1, 3.14 / 180, 150); // runs the actual detection
  cv::Mat newLine = cv::Mat::ones(cv::Size(candy.cols, candy.rows), CV_8U);

  for( size_t i = 0; i < lines.size(); i++ ) {
    float rho = lines[i][0];
    float theta = lines[i][1];
    double a = cos(theta), b = sin(theta);
    double x0 = a*rho, y0 = b*rho;
    cv::Point pt1(cvRound(x0 + 1000*(-b)), cvRound(y0 + 1000*(a)));
    cv::Point pt2(cvRound(x0 - 1000*(-b)), cvRound(y0 - 1000*(a)));
    cv::line(newLine, pt1, pt2, cv::Scalar(255,0,0), 1, 8);
  }

  cv::Canny(newLine, newLine, 50, 200, 3);
  cv::dilate(
    newLine,
    newLine,
    cv::getStructuringElement(cv::MORPH_ELLIPSE, cv::Size(5, 5))
  );

  std::vector<std::vector<cv::Point> > approxContours = [self findContours:newLine check: false] ;

  float height = candy.cols;
  float width = candy.rows;
  double MAX_COUNTOUR_AREA = (width - 10) * (height - 10);
    
  std::vector<std::vector<cv::Point> > approxContoursFilter;

  for (size_t i = 0; i < approxContours.size(); i++){
      if(
        approxContours[i].size() > 2
        && cv::contourArea(approxContours[i]) < MAX_COUNTOUR_AREA
        && cv::contourArea(approxContours[i]) > 10000){
          approxContoursFilter.push_back(approxContours[i]);
      }
    }

  cv::Mat newMat = cv::Mat::ones(cv::Size(candy.cols, candy.rows), CV_8U);
  cv::bitwise_not(newMat, newMat);

  cv::drawContours(newMat, approxContours, -1, cv::Scalar(0.0, 255.0, 0.0), -1);

  std::vector<std::vector<cv::Point> > approxContoursFinal = [self findContours:newLine check: true] ;
  std::vector<std::vector<cv::Point> > approxContoursFinalFilter;


  std::vector<cv::Point> largestArea;
  double maxArea = 0;
   for (size_t i = 0; i < approxContoursFinal.size(); i++){
      if(
        approxContoursFinal[i].size() == 4
        && cv::contourArea(approxContoursFinal[i]) < MAX_COUNTOUR_AREA
        && cv::contourArea(approxContoursFinal[i]) > 10000
        && cv::isContourConvex(approxContoursFinal[i])
        && cv::contourArea(approxContoursFinal[i]) > maxArea){
           largestArea = approxContoursFinal[i];
           maxArea = cv::contourArea(approxContoursFinal[i]);
      }
    }



  std::vector<cv::Point> rect = [self OrderPoints:largestArea];
  std::vector<cv::Point> points = [self convertPoints:rect photo:image];

 
  cv::Point topLeft = points.size() == 4 ? points[0] : cv::Point(cropperOffsetWhenCornersNotFound,cropperOffsetWhenCornersNotFound) ;
  cv::Point topRight = points.size() == 4 ? points[1] : cv::Point(image.size.width - cropperOffsetWhenCornersNotFound,cropperOffsetWhenCornersNotFound);
  cv::Point bottomRight = points.size() == 4 ? points[2] : cv::Point(image.size.width - cropperOffsetWhenCornersNotFound,image.size.height - cropperOffsetWhenCornersNotFound);
  cv::Point bottomLeft = points.size() == 4 ? points[3] : cv::Point(cropperOffsetWhenCornersNotFound, image.size.height -  cropperOffsetWhenCornersNotFound);

    NSDictionary *TOP_LEFT = @{
                    @"x":@(topLeft.x),
                    @"y":@(topLeft.y),
                    };
    NSDictionary *TOP_RIGHT = @{
                  @"x":@(topRight.x),
                  @"y":@(topRight.y),
                  };
    NSDictionary *BOTTOM_RIGHT = @{
                  @"x":@(bottomRight.x),
                  @"y":@(bottomRight.y),
                  };
    NSDictionary *BOTTOM_LEFT = @{
                  @"x":@(bottomLeft.x),
                  @"y":@(bottomLeft.y),
                  };
    NSDictionary *corners = @{
                  @"TOP_LEFT":TOP_LEFT,
                  @"TOP_RIGHT":TOP_RIGHT,
                  @"BOTTOM_RIGHT":BOTTOM_RIGHT,
                  @"BOTTOM_LEFT":BOTTOM_LEFT,
                  };
    NSDictionary *info = @{
                  @"corners":corners,
                  @"width":@(image.size.width),
                  @"height":@(image.size.height),
                  };

  resolve(info);
}

- (NSString *)convertMatToBase64:(cv::Mat) mat{
  UIImage *newImage = [self convertCVMatToUIImage:mat];
  NSString *base64 = [self encodeToBase64String:newImage quality:100] ;

  NSString *cleaned = [base64 stringByReplacingOccurrencesOfString: @"\\s+"
                                                             withString: @""
                                                                options: NSRegularExpressionSearch
                                                                  range: NSMakeRange(0, [base64 length])];
 return cleaned;
}

- (NSString *)convertUIImageToFile:(UIImage *)image quality:(int)quality{
  NSString *uuid = [NSUUID UUID].UUIDString;
  NSString *documentsDirectory = [NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, YES) lastObject];
  NSString *filePath = [documentsDirectory stringByAppendingPathComponent:@"LAYWERPRO_"];
  filePath = [filePath stringByAppendingString:uuid];
  filePath = [filePath stringByAppendingString:@".jpg"];

  NSData *imageData = UIImageJPEGRepresentation(image, quality?quality/100:1); 
  [imageData writeToFile:filePath atomically:YES];

  return filePath;
}

- (double) _distance:(cv::Point) p1 p2:(cv::Point) p2{
    return sqrt(((p2.x - p1.x)*(p2.x - p1.x)) + ((p2.y - p1.y)*(p2.y - p1.y)));
}

- (double) _calcPoint:(double) x limit:(double) limit{
    if(x < 0){
      return 0;
    }
    else if (x > limit) {
      return limit;
    }
    return x;
}

- (std::vector<cv::Point>) OrderPoints:(std::vector<cv::Point>) ip_op_corners_orig {
    if(ip_op_corners_orig.size()<4){
      return ip_op_corners_orig;
    }
  
    //Making a copy of the Original corner points
    std::vector<cv::Point> corners = ip_op_corners_orig;

    ip_op_corners_orig.clear();
    ip_op_corners_orig.resize(4);

    //Sorting based on the X Co-ordinates of points
    std::vector<int> sIdx = { 0, 1, 2, 3 };
    std::vector<cv::Point> leftMost, rightMost;

    std::sort(sIdx.begin(), sIdx.end(), [&corners](int i1, int i2){return corners[i1].x < corners[i2].x; });

    //Getting the Left most and Right most points and getting the top left and bottom left points
    leftMost = { corners[sIdx[0]], corners[sIdx[1]] };

    //Getting the Top Left and Bottom Left point
    ip_op_corners_orig[0] = leftMost[0].y > leftMost[1].y ? leftMost[1] : leftMost[0];
    ip_op_corners_orig[3] = leftMost[0].y < leftMost[1].y ? leftMost[1] : leftMost[0];


    //Getting the Bottom right anfd top right point
    rightMost = { corners[sIdx[2]], corners[sIdx[3]] };

    //Getting the Top right and Bottom right point
    ip_op_corners_orig[1] = rightMost[0].y > rightMost[1].y ? rightMost[1] : rightMost[0];
    ip_op_corners_orig[2] = rightMost[0].y < rightMost[1].y ? rightMost[1] : rightMost[0];


    return ip_op_corners_orig;
}
 
- (std::vector<cv::Point>) convertPoints:(std::vector<cv::Point>) ip_op_corners_orig photo:(UIImage *) photo{
    if(ip_op_corners_orig.size() < 4){
      return ip_op_corners_orig;
    }
  
    //Making a copy of the Original corner points
    std::vector<cv::Point> corners = ip_op_corners_orig;
    corners.resize(4);
    double ratio = photo.size.height / shrunkImageHeight;
    double borderSize = 10 * ratio;

    corners[0] = cv::Point([self _calcPoint:ip_op_corners_orig[0].x * ratio - borderSize limit:photo.size.width], [self _calcPoint:ip_op_corners_orig[0].y * ratio - borderSize limit:photo.size.height]); //topLeft
    corners[1] = cv::Point([self _calcPoint:ip_op_corners_orig[1].x * ratio + borderSize / 2 limit:photo.size.width], [self _calcPoint:ip_op_corners_orig[1].y * ratio - borderSize limit:photo.size.height]); //topRight
    corners[2] = cv::Point([self _calcPoint:ip_op_corners_orig[2].x * ratio + borderSize / 2 limit:photo.size.width], [self _calcPoint:ip_op_corners_orig[2].y * ratio limit:photo.size.height]); //bottomRight
    corners[3] = cv::Point([self _calcPoint:ip_op_corners_orig[3].x * ratio - borderSize limit:photo.size.width], [self _calcPoint:ip_op_corners_orig[3].y * ratio + borderSize / 2 limit:photo.size.height]); //bottomLeft

    return corners;
}
 
- (cv::Mat)convertUIImageToCVMat:(UIImage *)image
{
    cv::Mat imageMat;
    UIImageToMat(image, imageMat);
    if (
      image.imageOrientation == UIImageOrientationLeft
      || image.imageOrientation == UIImageOrientationRight
      ) {
        cv::rotate(imageMat, imageMat, cv::ROTATE_90_CLOCKWISE);
    }
   
    return imageMat;
}

- (UIImage *)convertCVMatToUIImage:(cv::Mat)cvMat
{
    UIImage *image = MatToUIImage(cvMat);
    return image;
}

- (UIImage *)pathToUIImage:(NSURL *)path {
  return [UIImage imageWithData:[NSData dataWithContentsOfURL:path]];
}

- (UIImage *)decodeBase64ToImage:(NSString *)strEncodeData {
  NSData *data = [[NSData alloc]initWithBase64EncodedString:strEncodeData options:NSDataBase64DecodingIgnoreUnknownCharacters];
  return [UIImage imageWithData:data];
}

- (NSString *)encodeToBase64String:(UIImage *)image quality:(int) quality{
 return [UIImageJPEGRepresentation(image, quality? quality/100 : 1) base64EncodedStringWithOptions:NSDataBase64Encoding64CharacterLineLength];
}

- (std::vector<std::vector<cv::Point> >) findContours:(cv::Mat) mat check:(BOOL) check {
    std::vector<std::vector<cv::Point> > contours;
    std::vector<cv::Vec4i> hierarchy;
    std::vector<std::vector<cv::Point>> approxs;
    cv::findContours( mat, contours, hierarchy, cv::RETR_TREE, cv::CHAIN_APPROX_SIMPLE );

    approxs.resize(contours.size());
    size_t i;
    for (i = 0; i < contours.size(); i++)
    {
      double peri = cv::arcLength(contours[i], true);
      cv::approxPolyDP(contours[i], approxs[i], 0.04 * peri, check);
    }

    return approxs;
}


@end


