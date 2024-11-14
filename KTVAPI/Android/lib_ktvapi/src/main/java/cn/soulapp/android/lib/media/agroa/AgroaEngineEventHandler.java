package cn.soulapp.android.lib.media.agroa;

import static io.agora.rtc2.Constants.CONNECTION_CHANGED_INTERRUPTED;
import static io.agora.rtc2.Constants.CONNECTION_STATE_CONNECTED;
import static io.agora.rtc2.Constants.CONNECTION_STATE_RECONNECTING;
import static io.agora.rtc2.Constants.QUALITY_BAD;
import static io.agora.rtc2.Constants.QUALITY_VBAD;
import static io.agora.rtc2.Constants.REMOTE_AUDIO_REASON_NETWORK_CONGESTION;
import static io.agora.rtc2.Constants.REMOTE_AUDIO_STATE_FROZEN;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;

import cn.soul.insight.log.core.SLogKt;
import cn.soulapp.android.lib.analyticsV2.SoulAnalyticsV2;
import io.agora.rtc2.Constants;
import io.agora.rtc2.IRtcEngineEventHandler;

public class AgroaEngineEventHandler {

    public AgroaEngineEventHandler() {
    }

    private RtcEngineHandler rtcEngineHandler;

    private boolean remoteLost;

    public void addEventHandler(RtcEngineHandler handler) {
        this.rtcEngineHandler = handler;
    }

    public void removeEventHandler(RtcEngineHandler handler) {
        this.rtcEngineHandler = null;
    }
    private final Map<String, Integer> netMap = new HashMap<>();

    public void clear(){
        netMap.clear();
    }

    public final IRtcEngineEventHandler rtcEventHandler = new IRtcEngineEventHandler() {
        private final static String TAG = "IRtcEngineEventHandler";
        @Override
        public void onFirstRemoteVideoDecoded(int uid, int width, int height, int elapsed) {
            Log.d(TAG, "onFirstRemoteVideoDecoded " + (uid & 0xFFFFFFFFL) + width + " " + height
                    + " " + elapsed);
            RtcEngineHandler rtcEngineHandler = AgroaEngineEventHandler.this.rtcEngineHandler;
            if (rtcEngineHandler != null) {
                rtcEngineHandler.onFirstRemoteVideoDecoded(uid, width, height, elapsed);
            }
        }

        @Override
        public void onFirstLocalVideoFrame(Constants.VideoSourceType source, int width, int height, int elapsed) {
            Log.d(TAG, "onFirstLocalVideoFrame " + width + " " + height + " " + elapsed);
        }

        @Override
        public void onAudioMixingFinished() {
            super.onAudioMixingFinished();
        }

        @Override
        public void onUserJoined(int uid, int elapsed) {
            Log.d(TAG, "onUserJoined ");
            RtcEngineHandler rtcEngineHandler = AgroaEngineEventHandler.this.rtcEngineHandler;
            if (rtcEngineHandler != null) {
                rtcEngineHandler.onUserJoined(uid, elapsed);
            }
        }

        @Override
        public void onUserOffline(int uid, int reason) {
            Log.d(TAG, "onUserOffline ");
            RtcEngineHandler rtcEngineHandler = AgroaEngineEventHandler.this.rtcEngineHandler;
            if (rtcEngineHandler != null) {
                rtcEngineHandler.onUserOffline(uid, reason);
            }
        }

        @Override
        public void onLeaveChannel(RtcStats stats) {
            super.onLeaveChannel(stats);
            RtcEngineHandler rtcEngineHandler = AgroaEngineEventHandler.this.rtcEngineHandler;
            if (rtcEngineHandler != null) {
                rtcEngineHandler.onLeaveChannel(stats);
            }
        }

        @Override
        public void onLocalAudioStats(LocalAudioStats stats) {
            super.onLocalAudioStats(stats);
            RtcEngineHandler rtcEngineHandler = AgroaEngineEventHandler.this.rtcEngineHandler;
            if (rtcEngineHandler != null) {
                rtcEngineHandler.onLocalAudioStats(stats);
            }
        }

        @Override
        public void onConnectionLost() {
            super.onConnectionLost();
            RtcEngineHandler rtcEngineHandler = AgroaEngineEventHandler.this.rtcEngineHandler;
            if (rtcEngineHandler != null) {
                rtcEngineHandler.onConnectionLost();
            }
        }

        @Override
        public void onAudioEffectFinished(int soundId) {
            super.onAudioEffectFinished(soundId);
            RtcEngineHandler rtcEngineHandler = AgroaEngineEventHandler.this.rtcEngineHandler;
            if (rtcEngineHandler != null) {
                rtcEngineHandler.onEffectPlayFinished();
            }
        }

        @Override
        public void onConnectionStateChanged(int state, int reason) {
            super.onConnectionStateChanged(state, reason);
            RtcEngineHandler rtcEngineHandler = AgroaEngineEventHandler.this.rtcEngineHandler;
            if(rtcEngineHandler != null){
                rtcEngineHandler.onConnectionStateChangedNew(state, reason);
            }
            if (rtcEngineHandler != null && state == CONNECTION_STATE_RECONNECTING
                    && reason == CONNECTION_CHANGED_INTERRUPTED) {
                rtcEngineHandler.onConnectionStateChanged(state);
            } else if (rtcEngineHandler != null && state == CONNECTION_STATE_CONNECTED) {
                rtcEngineHandler.onConnectionStateChanged(state);
            }
        }

        @Override
        public void onRemoteAudioStateChanged(int uid, int state, int reason, int elapsed) {
            super.onRemoteAudioStateChanged(uid, state, reason, elapsed);
            RtcEngineHandler rtcEngineHandler = AgroaEngineEventHandler.this.rtcEngineHandler;
            if (rtcEngineHandler != null) {
                if (state == REMOTE_AUDIO_STATE_FROZEN
                        && reason == REMOTE_AUDIO_REASON_NETWORK_CONGESTION) {
                    remoteLost = true;
                    rtcEngineHandler.onRemoteAudioBad();
                } else if (state < REMOTE_AUDIO_STATE_FROZEN) {
                    remoteLost = false;
                    rtcEngineHandler.onRemoteAudioGood();
                }
            }
        }

        @Override
        public void onRemoteSubscribeFallbackToAudioOnly(int uid, boolean isFallbackOrRecover) {
            super.onRemoteSubscribeFallbackToAudioOnly(uid, isFallbackOrRecover);
        }

        @Override
        public void onRemoteVideoStateChanged(int uid, int state, int reason, int elapsed) {
            super.onRemoteVideoStateChanged(uid, state, reason, elapsed);
            RtcEngineHandler rtcEngineHandler = AgroaEngineEventHandler.this.rtcEngineHandler;
            if (rtcEngineHandler != null) {
                rtcEngineHandler.onRemoteVideoStateChanged(uid, state, reason, elapsed);
            }
        }

        @Override
        public void onTokenPrivilegeWillExpire(String token) {
            super.onTokenPrivilegeWillExpire(token);
            RtcEngineHandler rtcEngineHandler = AgroaEngineEventHandler.this.rtcEngineHandler;
            if (rtcEngineHandler != null) {
                rtcEngineHandler.onTokenPrivilegeWillExpire(token);
            }
        }

        // 通话前网络状况监测
        @Override
        public void onLastmileQuality(int quality) {
            super.onLastmileQuality(quality);
            RtcEngineHandler rtcEngineHandler = AgroaEngineEventHandler.this.rtcEngineHandler;
            if (rtcEngineHandler != null) {
                rtcEngineHandler.onLastMileQuality(quality);
            }
        }

        // 通话中网络状况监测
        @Override
        public void onNetworkQuality(int uid, int txQuality, int rxQuality) {
            super.onNetworkQuality(uid, txQuality, rxQuality);
            RtcEngineHandler rtcEngineHandler = AgroaEngineEventHandler.this.rtcEngineHandler;
            if (rtcEngineHandler != null) {
                if (txQuality == QUALITY_BAD) {
                    rtcEngineHandler.onNetWorkBad(uid);
                } else if (txQuality == QUALITY_VBAD) {
                    rtcEngineHandler.onNetWorkTerrible(uid);
                } else if (txQuality < 4) {
                    if (uid == 0 || !remoteLost)
                        rtcEngineHandler.onNetWorkGood(uid);
                }
                rtcEngineHandler.onNetworkQuality(uid, txQuality, rxQuality);
            }

            int target = Math.max(txQuality, rxQuality);
            if(target >= QUALITY_BAD){
                Integer netWorkBadRecordCount = netMap.get(uid+"");
                int times = netWorkBadRecordCount == null ? 0 : netWorkBadRecordCount;
                if(times == 3){
                    Map<String, Object> map = new HashMap<>();
                    SoulAnalyticsV2.getInstance().onEvent(cn.soulapp.android.lib.analyticsV2.Const.EventType.INDICATORS, "RTC_Bad_Network", map);
                }
                times++;
                netMap.put(uid+"", times);
            }
        }

        @Override
        public void onError(int err) {
            super.onError(err);
            SLogKt.SLogApi.d("sl_rtcEngine", "--onError--err : " + err);
            Log.e(TAG, ("onError " + err));
            RtcEngineHandler rtcEngineHandler = AgroaEngineEventHandler.this.rtcEngineHandler;
            if (rtcEngineHandler != null) {
                rtcEngineHandler.onError(err);
            }
        }

        @Override
        public void onRemoteAudioStats(RemoteAudioStats stats) {
            super.onRemoteAudioStats(stats);
            RtcEngineHandler rtcEngineHandler = AgroaEngineEventHandler.this.rtcEngineHandler;
            if (rtcEngineHandler != null) {
                rtcEngineHandler.onRemoteAudioStats(stats);
            }
        }

        @Override
        public void onAudioMixingStateChanged(int state, int errorCode) {
            super.onAudioMixingStateChanged(state, errorCode);
            RtcEngineHandler rtcEngineHandler = AgroaEngineEventHandler.this.rtcEngineHandler;
            if (rtcEngineHandler != null) {
                rtcEngineHandler.onAudioMixingStateChanged(state, errorCode);
            }
        }

        @Override
        public void onAudioRouteChanged(int routing) {
            super.onAudioRouteChanged(routing);
            RtcEngineHandler rtcEngineHandler = AgroaEngineEventHandler.this.rtcEngineHandler;
            if (rtcEngineHandler != null) {
                rtcEngineHandler.onAudioRouteChanged(routing);
            }
        }

        @Override
        public void onAudioVolumeIndication(AudioVolumeInfo[] speakers, int totalVolume) {
            super.onAudioVolumeIndication(speakers, totalVolume);
            RtcEngineHandler rtcEngineHandler = AgroaEngineEventHandler.this.rtcEngineHandler;
            if (rtcEngineHandler != null) {
                rtcEngineHandler.onAudioVolumeIndication(speakers, totalVolume);
            }
        }

        @Override
        public void onStreamMessage(int uid, int streamId, byte[] data) {
            super.onStreamMessage(uid, streamId, data);
            RtcEngineHandler rtcEngineHandler = AgroaEngineEventHandler.this.rtcEngineHandler;
            if (rtcEngineHandler != null) {
                rtcEngineHandler.onStreamMessage(uid, streamId, data);
            }
        }

        @Override
        public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
            SLogKt.SLogApi.d("sl_rtcEngine", "--onJoinChannelSuccess--channel : " + channel);
            Log.d(TAG, ("onJoinChannelSuccess " + channel + " " + uid + " " + (uid & 0xFFFFFFFFL)
                    + " " + elapsed));
            RtcEngineHandler rtcEngineHandler = AgroaEngineEventHandler.this.rtcEngineHandler;
            if (rtcEngineHandler != null) {
                rtcEngineHandler.onJoinChannelSuccess(channel, uid, elapsed);
            }
        }

        public void onRejoinChannelSuccess(String channel, int uid, int elapsed) {
            Log.d(TAG, ("onRejoinChannelSuccess " + channel + " " + uid + " " + elapsed));
            RtcEngineHandler rtcEngineHandler = AgroaEngineEventHandler.this.rtcEngineHandler;
            if (rtcEngineHandler != null) {
                rtcEngineHandler.onRejoinChannelSuccess(channel, uid, elapsed);
            }
        }

        @Override
        public void onAudioQuality(int uid, int quality, short delay, short lost) {
            super.onAudioQuality(uid, quality, delay, lost);
            RtcEngineHandler rtcEngineHandler = AgroaEngineEventHandler.this.rtcEngineHandler;
            if (rtcEngineHandler != null) {
                rtcEngineHandler.onAudioQuality(uid, quality, delay, lost);
            }
        }

        @Override
        public void onRemoteVideoStats(RemoteVideoStats stats) {
            super.onRemoteVideoStats(stats);
            RtcEngineHandler rtcEngineHandler = AgroaEngineEventHandler.this.rtcEngineHandler;
            if (rtcEngineHandler != null) {
                rtcEngineHandler.onRemoteVideoStats(stats);
            }
        }

        public void onWarning(int warn) {
            Log.w(TAG, ("onWarning " + warn));
        }
    };

}
