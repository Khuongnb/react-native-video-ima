//
//  MuxTracking.h
//  Pods
//
//  Created by Khuong on 4/8/21.
//

#ifndef MuxTracking_h
#define MuxTracking_h


#endif /* MuxTracking_h */
@import UIKit;
#include <AVFoundation/AVFoundation.h>

@interface MuxTracking : NSObject
- (instancetype)initWithConfig:(NSDictionary*)muxConfig;
- (void)startTracking:(AVPlayerLayer*)playerLayer;
@end
