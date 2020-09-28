#import "IMAAds.h"
#import <GoogleInteractiveMediaAds/GoogleInteractiveMediaAds.h>
#import <React/RCTLog.h>

@implementation IMAAds {
    IMAAdsLoader* _adLoader;
    IMAAdsManager* _adManager;
    IMAAVPlayerContentPlayhead* _contentPlayhead;
    UIViewController* _adViewController;
    UIView* _playerView;
    AVPlayer* _player;
}

- (id)initWithSettings:(NSDictionary *)settings player:(AVPlayer *)player playerView:(UIView *)playerView adViewController:(UIViewController *)adViewController {
    _player = player;
    _playerView = playerView;
    _adViewController = adViewController;
    [self initAds];
    return self;
}
    
- (void)initAds {
    _adLoader = [[IMAAdsLoader alloc] initWithSettings:nil];
    _adLoader.delegate = self;
}

- (void)requestAds:(NSString*)adTag {
    _contentPlayhead = [[IMAAVPlayerContentPlayhead alloc] initWithAVPlayer:_player];
    IMAAdDisplayContainer *adDisplayContainer = [[IMAAdDisplayContainer alloc] initWithAdContainer:_playerView viewController:_adViewController companionSlots:nil];

    
    IMAAdsRequest *request = [[IMAAdsRequest alloc] initWithAdTagUrl:adTag
                                                  adDisplayContainer:adDisplayContainer
                                                     contentPlayhead:_contentPlayhead
                                                         userContext:nil];
    [_adLoader requestAdsWithRequest:request];
}

- (void)adsLoader:(IMAAdsLoader *)loader adsLoadedWithData:(IMAAdsLoadedData *)adsLoadedData {
    _adManager = adsLoadedData.adsManager;
    _adManager.delegate = self;

    IMAAdsRenderingSettings *adsRenderingSettings = [[IMAAdsRenderingSettings alloc] init];
    // TODO: More settings goes here
    adsRenderingSettings.webOpenerPresentingController = _adViewController;

    [_adManager initializeWithAdsRenderingSettings:adsRenderingSettings];
    [_adManager start];
}

- (void)adsLoader:(IMAAdsLoader *)loader failedWithErrorData:(IMAAdLoadingErrorData *)adErrorData {
    NSLog(@"Ads ERROR %@", adErrorData.adError.message);
    if (self.onAdEvent)
        self.onAdEvent(@{@"type": @"ERROR",
                         @"message": adErrorData.adError.message
                       });
}

- (void)adsManager:(IMAAdsManager *)adsManager didReceiveAdError:(IMAAdError *)error {
    NSLog(@"Ads manager error %@", error.message);
    if (self.onAdEvent)
        self.onAdEvent(@{@"type": @"ERROR",
                         @"message": error.message
                       });
}

- (void)adsManager:(IMAAdsManager *)adsManager didReceiveAdEvent:(IMAAdEvent *)event {
    if (!self.onAdEvent)
        return;
    if (event.type == kIMAAdEvent_LOADED) {
        self.onAdEvent(@{@"type": @"LOADED"});
    }
    if (event.type == kIMAAdEvent_STARTED) {
        self.onAdEvent(@{@"type": @"STARTED"});
    }
    if (event.type == kIMAAdEvent_COMPLETE) {
        self.onAdEvent(@{@"type": @"COMPLETE"});
    }
    if (event.type == kIMAAdEvent_TAPPED) {
        self.onAdEvent(@{@"type": @"TAPPED"});
    }
    if (event.type == kIMAAdEvent_CLICKED) {
        self.onAdEvent(@{@"type": @"CLICKED"});
    }
    if (event.type == kIMAAdEvent_PAUSE) {
        self.onAdEvent(@{@"type": @"PAUSED"});
    }
    if (event.type == kIMAAdEvent_RESUME) {
        self.onAdEvent(@{@"type": @"RESUME"});
    }
}

- (void)adsManagerDidRequestContentPause:(IMAAdsManager *)adsManager {
    [_player pause];
}

- (void)adsManagerDidRequestContentResume:(IMAAdsManager *)adsManager {
    [_player play];
}

- (void)dispatch:(NSString *)action payload:(id)payload{
    if ([action isEqualToString:@"REQUEST_ADS"]) {
        if ([payload isKindOfClass:[NSString class]]) {
            [self requestAds:(NSString*) payload];
        } else {
            RCTLogError(@"Invalid payload, request ads require <string> value");
        }
    }
    if ([action isEqualToString:@"START_AD"]) {
        [_adManager start];
    }
    if ([action isEqualToString:@"PAUSE_AD"]) {
        [_adManager pause];
    }
    if ([action isEqualToString:@"RESUME_AD"]) {
        [_adManager resume];
    }
    if ([action isEqualToString:@"SET_AD_VOLUME"]) {
        if ([payload isKindOfClass:[NSNumber class]]) {
            _adManager.volume = [payload floatValue];
        } else {
            RCTLogError(@"Invalid payload, set volume require <float> value from 0 to 1");
        }
    }
    if ([action isEqualToString:@"CONTENT_COMPLETE"]) {
        [_adLoader contentComplete];
    }
}
@end
