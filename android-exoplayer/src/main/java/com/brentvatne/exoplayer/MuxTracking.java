package com.brentvatne.exoplayer;

import android.content.Context;

import com.facebook.react.bridge.ReadableMap;
import com.google.ads.interactivemedia.v3.api.AdsLoader;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Player;
import com.mux.stats.sdk.core.model.CustomerPlayerData;
import com.mux.stats.sdk.core.model.CustomerVideoData;
import com.mux.stats.sdk.core.model.CustomerViewData;
import com.mux.stats.sdk.core.model.CustomerData;
import com.mux.stats.sdk.muxstats.MuxStatsExoPlayer;

import java.util.Arrays;
import java.util.HashMap;

public class MuxTracking {

    private final String CONFIG_ENVIRONMENT_KEY = "environment_key";
    private final String CONFIG_PLAYER_NAME = "player_name";
    private final String CONFIG_VIEW_USER_ID = "user_id";
    private final String CONFIG_VIEW_SUBSCRIPTION_DETAIL = "sub_property";
    private final String CONFIG_VIDEO_TITLE = "video_title";
    private final String CONFIG_VIDEO_CONTENT_TYPE = "video_content_type";
    private final String CONFIG_VIDEO_LANGUAGE_CODE = "video_language_code";
    private final String CONFIG_VIEW_SESSION_ID = "view_session_id";


    private final String[] REQUIRED_CONFIG = {
            CONFIG_ENVIRONMENT_KEY
    };

    private final Context activityContext;
    private MuxStatsExoPlayer tracker;

    MuxTracking(Context context) {
        activityContext = context;
    }

    private String getKeyFrom(ReadableMap map, String key, String fallbackValue) throws Exception {
        String value = map.hasKey(key) ? map.getString(key) : null;
        if (value == null) {
            if (Arrays.asList(REQUIRED_CONFIG).contains(key)) {
                throw new Exception("Mux config key " + key +" is required");
            }
            value = fallbackValue;
        }
        return value;
    }

    private CustomerPlayerData getPlayerData(ReadableMap muxConfig) throws Exception {
        CustomerPlayerData customerPlayerData = new CustomerPlayerData();
        customerPlayerData.setEnvironmentKey(getKeyFrom(muxConfig, CONFIG_ENVIRONMENT_KEY, null));

        String playerName = getKeyFrom(muxConfig, CONFIG_PLAYER_NAME, "vimai-mobile-exoplayer");
        customerPlayerData.setPlayerName(playerName);

        String viewerUserId = getKeyFrom(muxConfig, CONFIG_VIEW_USER_ID, null);
        if (viewerUserId != null) {
            customerPlayerData.setViewerUserId(viewerUserId);
        }

        String viewerSubscriptionDetail = getKeyFrom(muxConfig, CONFIG_VIEW_SUBSCRIPTION_DETAIL, null);
        if (viewerUserId != null) {
            customerPlayerData.setSubPropertyId(viewerSubscriptionDetail);
        }

        return customerPlayerData;
    }

    private CustomerVideoData getVideoData(ReadableMap muxConfig) throws Exception {
        CustomerVideoData customerVideoData = new CustomerVideoData();

        String videoTitle = getKeyFrom(muxConfig, CONFIG_VIDEO_TITLE, null);
        if (videoTitle != null) {
            customerVideoData.setVideoTitle(videoTitle);
        }
        String videoContentType = getKeyFrom(muxConfig, CONFIG_VIDEO_CONTENT_TYPE, null);
        if (videoTitle != null) {
            customerVideoData.setVideoContentType(videoContentType);
        }
        String videoLanguageCode = getKeyFrom(muxConfig, CONFIG_VIDEO_LANGUAGE_CODE, null);
        if (videoTitle != null) {
            customerVideoData.setVideoLanguageCode(videoLanguageCode);
        }
        return customerVideoData;
    }

    private CustomerViewData getViewData(ReadableMap muxConfig) throws Exception {
        CustomerViewData customerViewData = new CustomerViewData();

        String viewSession = getKeyFrom(muxConfig, CONFIG_VIEW_SESSION_ID, null);
        if (viewSession != null) {
            customerViewData.setViewSessionId(viewSession);
        }

        return customerViewData;
    }

    public void track(ExoPlayer player, ReadableMap muxConfig) throws IllegalArgumentException {
        this.release();
        String playerName = muxConfig.hasKey(CONFIG_PLAYER_NAME) ? muxConfig.getString(CONFIG_PLAYER_NAME) : "vimai-mobile-exoplayer";
        try {
            CustomerData data = new CustomerData(
                getPlayerData(muxConfig), getVideoData(muxConfig), getViewData(muxConfig)
            );
            tracker = new MuxStatsExoPlayer(
                    activityContext,
                    player,
                    playerName,
                    data
            );
            System.out.println("==== Mux start tracking video");
        }
        catch (Exception e) {
            tracker = null;
            System.out.println("==== Mux error: " + e.getMessage());
            throw new IllegalArgumentException(e.getMessage());
        }

    }

    public void setStreamType(int type) {
        if (tracker != null) {
            tracker.setStreamType(type);
        }
    }

    public void monitorImaAdsLoader(AdsLoader adsLoader) {
        if (tracker != null && adsLoader != null) {
            tracker.monitorImaAdsLoader(adsLoader);
        }
    }

    public void release() {
        if (tracker != null) {
            tracker.release();
        }
    }
}
