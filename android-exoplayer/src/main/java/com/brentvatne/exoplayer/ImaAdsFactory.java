package com.brentvatne.exoplayer;

/*
*
*  com.brentvatne.exoplayer.ImaAdsFactory
*  react-native-video-ima
*
*  Created by Khuong (khuongnb1997@gmail.com) on 9/24/20.
*  Copyright © 2020 Facebook. All rights reserved.
*
*/

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.view.ViewGroup;

import com.facebook.react.bridge.Dynamic;
import com.google.ads.interactivemedia.v3.api.AdDisplayContainer;
import com.google.ads.interactivemedia.v3.api.AdError;
import com.google.ads.interactivemedia.v3.api.AdErrorEvent;
import com.google.ads.interactivemedia.v3.api.AdEvent;
import com.google.ads.interactivemedia.v3.api.AdPodInfo;
import com.google.ads.interactivemedia.v3.api.AdsLoader;
import com.google.ads.interactivemedia.v3.api.AdsManager;
import com.google.ads.interactivemedia.v3.api.AdsManagerLoadedEvent;
import com.google.ads.interactivemedia.v3.api.AdsRenderingSettings;
import com.google.ads.interactivemedia.v3.api.AdsRequest;
import com.google.ads.interactivemedia.v3.api.ImaSdkFactory;
import com.google.ads.interactivemedia.v3.api.ImaSdkSettings;
import com.google.ads.interactivemedia.v3.api.player.AdMediaInfo;
import com.google.ads.interactivemedia.v3.api.player.VideoAdPlayer;
import com.google.ads.interactivemedia.v3.api.player.VideoProgressUpdate;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.MediaSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;

public class ImaAdsFactory implements
        AdsLoader.AdsLoadedListener,
        AdErrorEvent.AdErrorListener,
        AdEvent.AdEventListener
{
    private AdsLoader _adsLoader;
    private ImaSdkFactory _sdkFactory;
    private SimpleExoPlayer _player;
    private VideoAdPlayer _videoAdPlayer;
    private AdsManager _adsManager;
    private AdMediaInfo _adMediaInfo;
    private Callable<MediaSource> _adMediaBuilder;
    private MediaSource _playerMediaSource;
    private Boolean _isAdDisplaying = false;
    private Player.EventListener _playerListenerForAd;
    private Player.EventListener _playerListener;
    private List<VideoAdPlayer.VideoAdPlayerCallback> callbacks = new ArrayList<>(1);
    private int _savedWindowIndex;
    private long _savedPlayerPosition;
    private float _savedPlayerVolume;
    private Timer timer;
    private ViewGroup _parent;
    private VideoEventEmitter _eventEmitter;

    ImaAdsFactory(Context context,
                  ViewGroup adUiContainer,
                  ViewGroup parent,
                  VideoEventEmitter eventEmitter) {
        _sdkFactory = ImaSdkFactory.getInstance();
        _parent = parent;
        _eventEmitter = eventEmitter;

        ImaSdkSettings settings = _sdkFactory.createImaSdkSettings();
        settings.setPlayerType("ODVAdsPlayer");
        _videoAdPlayer = createVideoAdPlayer();

        AdDisplayContainer adDisplayContainer = _sdkFactory.createAdDisplayContainer();
        adDisplayContainer.setAdContainer(adUiContainer);
        adDisplayContainer.setPlayer(_videoAdPlayer);

        _adsLoader = _sdkFactory.createAdsLoader(context, settings, adDisplayContainer);
        _playerListenerForAd = createPlayerEventListener();
    }

    public void dispatch(String action, Dynamic payload) throws IllegalArgumentException {
        switch (action) {
            case "REQUEST_ADS":
                try {
                    requestAdsAndStart(payload.asString());
                } catch (Exception e) {
                    throw new IllegalArgumentException("Require <string> ad tag");
                }
                break;
            case "START_AD":
                _adsManager.start();
                break;
            case "PAUSE_AD":
                _adsManager.pause();
                break;
            case "RESUME_AD":
                _adsManager.resume();
                break;
            case "SET_AD_VOLUME":
                try {
                    _player.setVolume((float)payload.asDouble());
                } catch (Exception e) {
                    throw new IllegalArgumentException("Set volume require <float> 0..1");
                }
                break;
            case "CONTENT_COMPLETE":
                if (_adsLoader != null) {
                    _adsLoader.contentComplete();
                }
            default:
                break;
        }
    }

    public void setPlayer(SimpleExoPlayer player,
                          Player.EventListener listener,
                          Callable<MediaSource> adMediaBuilder,
                          MediaSource playerMediaSource) {
        _player = player;
        _playerListener = listener;
        _adMediaBuilder = adMediaBuilder;
        _playerMediaSource = playerMediaSource;
    }

    public Uri getAdUri() {
        return Uri.parse(_adMediaInfo.getUrl());
    }

    public Boolean getIsAdDisplaying() {
        return _isAdDisplaying;
    }

    public void requestAdsAndStart(String adTag) {
        _adsLoader.addAdsLoadedListener(this);
        _adsLoader.addAdErrorListener(this);
        AdsRequest request = buildAdRequest(adTag);
        _adsLoader.requestAds(request);
    }

    private VideoAdPlayer createVideoAdPlayer() {
        return new VideoAdPlayer() {
            @Override
            public void loadAd(AdMediaInfo adMediaInfo, AdPodInfo adPodInfo) {
                _adMediaInfo = adMediaInfo;
                try {
                    _player.prepare(_adMediaBuilder.call());
                } catch (Exception e) {
                    System.out.println("Ads not ready");
                }
            }

            @Override
            public void playAd(AdMediaInfo adMediaInfo) {
                _player.removeListener(_playerListener);
                _player.addListener(_playerListenerForAd);
                _parent.requestLayout();
                _player.setPlayWhenReady(true);
                _isAdDisplaying = true;
                startTracking();
            }

            @Override
            public void pauseAd(AdMediaInfo adMediaInfo) {
                _player.setPlayWhenReady(false);
                stopTracking();
            }

            @Override
            public void stopAd(AdMediaInfo adMediaInfo) {
                _player.removeListener(_playerListenerForAd);
                _player.addListener(_playerListener);
                _isAdDisplaying = false;
                stopTracking();
            }

            @Override
            public void release() {

            }

            @Override
            public void addCallback(VideoAdPlayerCallback videoAdPlayerCallback) {
                callbacks.add(videoAdPlayerCallback);
            }

            @Override
            public void removeCallback(VideoAdPlayerCallback videoAdPlayerCallback) {
                callbacks.remove(videoAdPlayerCallback);
            }

            @Override
            public VideoProgressUpdate getAdProgress() {
                if (_player == null)
                    return VideoProgressUpdate.VIDEO_TIME_NOT_READY;
                return new VideoProgressUpdate(_player.getCurrentPosition(), _player.getDuration());
            }

            @Override
            public int getVolume() {
                if (_player == null) return 0;
                return (int)_player.getVolume();
            }
        };
    }

    private void startTracking() {
        if (timer != null) {
            return;
        }
        timer = new Timer();
        TimerTask updateTimerTask =
                new TimerTask() {
                    @Override
                    public void run() {
                        // Tell IMA the current video progress. A better implementation would be
                        // reactive to events from the media player, instead of polling.
                        for (VideoAdPlayer.VideoAdPlayerCallback callback : callbacks) {
                            callback.onAdProgress(_adMediaInfo, _videoAdPlayer.getAdProgress());
                        }
                    }
                };
        int initialDelayMs = 250;
        int pollingTimeMs = 250;
        timer.schedule(updateTimerTask, pollingTimeMs, initialDelayMs);
    }

    private void stopTracking() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    private Player.EventListener createPlayerEventListener() {
        return new Player.EventListener() {
            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                switch (playbackState) {
                    case Player.STATE_IDLE:
                        break;
                    case Player.STATE_BUFFERING:
                        break;
                    case Player.STATE_READY:
                        for (VideoAdPlayer.VideoAdPlayerCallback callback: callbacks) {
                            if (playWhenReady)
                                callback.onResume(_adMediaInfo);
                            else
                                callback.onPause(_adMediaInfo);
                        }
                        break;
                    case Player.STATE_ENDED:
                        for (VideoAdPlayer.VideoAdPlayerCallback callback: callbacks) {
                            callback.onEnded(_adMediaInfo);
                        }
                        break;
                    default:
                        break;
                }
            }
        };
    }

    private AdsRequest buildAdRequest(String adTag) {
        AdsRequest request = _sdkFactory.createAdsRequest();
        request.setAdTagUrl(adTag);
        request.setContentProgressProvider(()->{
            if (_player == null)
                return VideoProgressUpdate.VIDEO_TIME_NOT_READY;
            return new VideoProgressUpdate(_player.getCurrentPosition(), _player.getDuration());
        });
        return request;
    }

    @Override
    public void onAdError(AdErrorEvent adErrorEvent) {
        Log.i("AdERROR: ", adErrorEvent.getError().getMessage());
    }

    @Override
    public void onAdsManagerLoaded(AdsManagerLoadedEvent adsManagerLoadedEvent) {
        AdsRenderingSettings renderingSettings = _sdkFactory.createAdsRenderingSettings();
        renderingSettings.setPlayAdsAfterTime(-1);
        _adsManager = adsManagerLoadedEvent.getAdsManager();
        _adsManager.addAdEventListener(this);
        _adsManager.addAdErrorListener(this);
        _adsManager.init(renderingSettings);
    }

    @Override
    public void onAdEvent(AdEvent adEvent) {
        if (_eventEmitter != null) {
            switch (adEvent.getType()) {
                case LOADED:
                    _eventEmitter.adEvent("LOADED");
                    break;
                case STARTED:
                    _eventEmitter.adEvent("STARTED");
                    break;
                case COMPLETED:
                    _eventEmitter.adEvent("COMPLETED");
                    break;
                case TAPPED:
                    _eventEmitter.adEvent("TAPPED");
                    break;
                case CLICKED:
                    _eventEmitter.adEvent("CLICKED");
                    break;
                case PAUSED:
                    _eventEmitter.adEvent("PAUSED");
                    break;
                case RESUMED:
                    _eventEmitter.adEvent("RESUME");
                    break;
            }
        }
        switch (adEvent.getType()) {
            case CONTENT_PAUSE_REQUESTED:
                _savedPlayerPosition = _player.getCurrentPosition();
                _savedWindowIndex = _player.getCurrentWindowIndex();
                _savedPlayerVolume = _player.getVolume();
                break;
            case CONTENT_RESUME_REQUESTED:
                try {
                    _player.prepare(_playerMediaSource);
                    _player.seekTo(_savedWindowIndex, _savedPlayerPosition);
                    _player.setVolume(_savedPlayerVolume);
                } catch (Exception e) {
                    System.out.println("Error while fallback");
                }
                break;
            case TAPPED:
                break;
            default:
                break;
        }
    }
}
