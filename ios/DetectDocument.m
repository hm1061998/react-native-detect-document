#import <React/RCTBridgeModule.h>

@interface RCT_EXTERN_MODULE(DetectDocument, NSObject)

RCT_EXTERN_METHOD(multiply:(float)a withB:(float)b
                 withResolver:(RCTPromiseResolveBlock)resolve
                 withRejecter:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(findDocumentCorners:(string)originalPhotoPath 
                 withResolver:(RCTPromiseResolveBlock)resolve
                 withRejecter:(RCTPromiseRejectBlock)reject)                

+ (BOOL)requiresMainQueueSetup
{
  return NO;
}

@end
