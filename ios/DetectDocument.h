
#ifdef RCT_NEW_ARCH_ENABLED
#import "RNDetectDocumentSpec.h"

@interface DetectDocument : NSObject <NativeDetectDocumentSpec>
#else
#import <React/RCTBridgeModule.h>

@interface DetectDocument : NSObject <RCTBridgeModule>
#endif

@end
