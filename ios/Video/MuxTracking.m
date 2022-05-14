//
//  MuxTracking.m
//  react-native-video-ima
//
//  Created by Khuong on 4/8/21.
//

#import <Foundation/Foundation.h>
@import MUXSDKStats;
#import "MuxTracking.h"
#import <React/RCTLog.h>

@implementation MuxTracking {
    MUXSDKCustomerPlayerData* customerPlayerData;
    MUXSDKCustomerVideoData* customerVideoData;
    MUXSDKCustomerViewData* customerViewData;
}

static NSString * const CONFIG_ENVIRONMENT_KEY = @"environment_key";
static NSString * const CONFIG_PLAYER_NAME = @"player_name";
static NSString * const CONFIG_VIEW_USER_ID = @"user_id";
static NSString * const CONFIG_VIEW_SUBSCRIPTION_DETAIL = @"sub_property";
static NSString * const CONFIG_VIDEO_TITLE = @"video_title";
static NSString * const CONFIG_VIDEO_CONTENT_TYPE = @"video_content_type";
static NSString * const CONFIG_VIDEO_LANGUAGE_CODE = @"video_language_code";
static NSString * const CONFIG_VIEW_SESSION_ID = @"view_session_id";

static NSString * const REQUIRED_CONFIG[] = {CONFIG_ENVIRONMENT_KEY};

- (NSString*)getKeyFrom:(NSDictionary *)muxConfig key:(NSString*)key fallback:(NSString*)fallback {
    NSUInteger count = sizeof(REQUIRED_CONFIG) / sizeof( REQUIRED_CONFIG[0]);
    NSArray * requiredConfig = [NSArray arrayWithObjects:REQUIRED_CONFIG count:count];
    if ([muxConfig objectForKey:key]) {
        return muxConfig[key];
    } else {
        if ([requiredConfig containsObject:key]) {
            RCTLogError(@"%@", [NSString stringWithFormat:@"MuxConfig key %@ is required", key]);
            return nil;
        } else {
            return fallback;
        }
    }
}

- (void)Logger:(NSString *)str {
    NSLog(@"==== VIMAI: %@", str);
}

- (void)createCustomerPlayerData:(NSDictionary *)muxConfig {
    customerPlayerData = [[MUXSDKCustomerPlayerData alloc] initWithEnvironmentKey:[self getKeyFrom:muxConfig key:CONFIG_ENVIRONMENT_KEY fallback:nil]];
    NSString* playerName = [self getKeyFrom:muxConfig key:CONFIG_PLAYER_NAME fallback:@"iOS Vimai Player"];
    if (playerName) {
        customerPlayerData.playerName = playerName;
    }
    NSString* userId = [self getKeyFrom:muxConfig key:CONFIG_VIEW_USER_ID fallback:nil];
    if (userId) {
        customerPlayerData.viewerUserId = userId;
    }
    NSString* subPropertyId = [self getKeyFrom:muxConfig key:CONFIG_VIEW_SUBSCRIPTION_DETAIL fallback:nil];
    if (subPropertyId) {
        customerPlayerData.subPropertyId = subPropertyId;
    }
}

- (void)createCustomerVideoData:(NSDictionary *)muxConfig {
    customerVideoData = [MUXSDKCustomerVideoData new];
    NSString* videoTitle = [self getKeyFrom:muxConfig key:CONFIG_VIDEO_TITLE fallback:nil];
    if (videoTitle) {
        customerVideoData.videoTitle = videoTitle;
    }
    NSString* videoContentType = [self getKeyFrom:muxConfig key:CONFIG_VIDEO_CONTENT_TYPE fallback:nil];
    if (videoContentType) {
        customerVideoData.videoContentType = videoContentType;
    }
    NSString* videoLanguageCode = [self getKeyFrom:muxConfig key:CONFIG_VIDEO_LANGUAGE_CODE fallback:nil];
    if (videoLanguageCode) {
        customerVideoData.videoLanguageCode = videoLanguageCode;
    }
}

- (void)createCustomerViewData:(NSDictionary *)muxConfig {
    customerViewData = [MUXSDKCustomerViewData new];
    NSString* viewSessionId = [self getKeyFrom:muxConfig key:CONFIG_VIDEO_LANGUAGE_CODE fallback:nil];
    if (viewSessionId) {
        customerViewData.viewSessionId = viewSessionId;
    }
}

- (instancetype)initWithConfig:(NSDictionary *)muxConfig {
    [self createCustomerPlayerData:muxConfig];
    [self createCustomerVideoData:muxConfig];
    [self createCustomerViewData:muxConfig];
    return self;
}

- (void)startTracking:(AVPlayerLayer *)playerLayer {
    if (!customerPlayerData || !customerVideoData) {
        RCTLogError(@"MuxTracking need initialize. Disable tracking");
        return;
    }
    [self Logger:[NSString stringWithFormat:@"Mux start tracking player %@ on video %@", customerPlayerData.playerName, customerVideoData.videoTitle]];
    [MUXSDKStats monitorAVPlayerLayer:playerLayer
                       withPlayerName:customerPlayerData.playerName
                           playerData:customerPlayerData
                            videoData:customerVideoData
                             viewData:customerViewData];
}

@end
