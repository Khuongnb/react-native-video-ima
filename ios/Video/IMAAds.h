//
//  IMAds.h
//  RCTVideo
//
//  Created by Khuong (khuongnb1997@gmail.com) on 9/22/20.
//  Copyright © 2020 Facebook. All rights reserved.

@import UIKit;
#import <GoogleInteractiveMediaAds/GoogleInteractiveMediaAds.h>
#import "RCTVideo.h"

@interface IMAAds : NSObject <IMAAdsLoaderDelegate, IMAAdsManagerDelegate>

@property (nonatomic, weak) RCTDirectEventBlock onAdEvent;

- (instancetype)initWithSettings:(NSDictionary* )settings
                          player:(AVPlayer* )player
                      playerView:(UIView* )playerView
                adViewController:(UIViewController* )adViewController;
- (void)requestAds:(NSString*)adTag;
- (void)dispatch:(NSString *)action payload:(id)payload;
@end
