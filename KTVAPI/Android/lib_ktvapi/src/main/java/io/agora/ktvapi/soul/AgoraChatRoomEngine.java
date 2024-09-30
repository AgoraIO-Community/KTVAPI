package io.agora.ktvapi.soul;

import android.app.Application;
import android.content.Context;
import android.media.AudioManager;
import android.opengl.EGLContext;
import android.opengl.Matrix;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;

import com.zego.chatroom.manager.entity.ResultCode;
import com.zego.chatroom.manager.room.ZegoLoginRoomCallback;
import com.zego.chatroom.manager.room.ZegoRoomLoginEvent;
import com.zego.zegoavkit2.audioplayer.IZegoAudioPlayerCallback;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import cn.mate.android.config.SConfiger;
import cn.soul.insight.log.core.SLogKt;
import cn.soulapp.android.lib.analyticsV2.SoulAnalyticsV2;
import cn.soulapp.android.lib.media.Const;
import cn.soulapp.android.lib.media.IAudioPlayerCallBack;
import cn.soulapp.android.lib.media.SLMediaPlayerState;
import cn.soulapp.android.lib.media.agroa.AgroaEngineConfig;
import cn.soulapp.android.lib.media.agroa.BuildConfig;
import cn.soulapp.android.lib.media.agroa.RtcEngineHandler;
import cn.soulapp.android.lib.media.agroa.SAaoraInstance;
import cn.soulapp.android.lib.media.rtc.SoulRtcEngine;
import cn.soulapp.android.lib.media.volcengine.GameParams;
import cn.soulapp.android.lib.media.zego.MediaPlayerCustomDataProvider;
import cn.soulapp.android.lib.media.zego.SLExternalVideoSourceType;
import cn.soulapp.android.lib.media.zego.SVideoDimension;
import cn.soulapp.android.lib.media.zego.SoulRTCSimulcastStreamModel;
import cn.soulapp.android.lib.media.zego.beans.AudioRecordingParams;
import cn.soulapp.android.lib.media.zego.beans.PlayKTVParams;
import cn.soulapp.android.lib.media.zego.beans.RemoteViewParams;
import cn.soulapp.android.lib.media.zego.beans.StreamMessage;
import cn.soulapp.android.lib.media.zego.interfaces.IAgoraKTVSyncProcess;
import cn.soulapp.android.lib.media.zego.interfaces.IAudioRecordCallBack;
import cn.soulapp.android.lib.media.zego.interfaces.IChatRoomEngine;
import cn.soulapp.android.lib.media.zego.interfaces.IEffectPlayCallBack;
import cn.soulapp.android.lib.media.zego.interfaces.IEngineInitCallback;
import cn.soulapp.android.lib.media.zego.interfaces.IMediaPlayerDecryptBlock;
import cn.soulapp.android.lib.media.zego.interfaces.IMusicMediaPlayerCallBack;
import cn.soulapp.android.lib.media.zego.interfaces.IMusicPlayCallback;
import cn.soulapp.android.lib.media.zego.interfaces.IRoomCallback;
import cn.soulapp.android.lib.media.zego.interfaces.IRoomLiveStatusCallback;
import cn.soulapp.android.lib.media.zego.interfaces.IUserIdDES;
import cn.soulapp.android.lib.media.zego.interfaces.IVideoStateCallBack;
import cn.soulapp.android.lib.media.zego.utils.GsonUtils;
import io.agora.base.TextureBuffer;
import io.agora.base.TextureBufferHelper;
import io.agora.base.VideoFrame;
import io.agora.base.internal.video.EglBase14;
import io.agora.base.internal.video.RendererCommon;
import io.agora.mediaplayer.IMediaPlayer;
import io.agora.mediaplayer.IMediaPlayerObserver;
import io.agora.mediaplayer.data.PlayerUpdatedInfo;
import io.agora.mediaplayer.data.SrcInfo;
import io.agora.rtc2.AgoraMediaRecorder;
import io.agora.rtc2.ChannelMediaOptions;
import io.agora.rtc2.Constants;
import io.agora.rtc2.DataStreamConfig;
import io.agora.rtc2.IAudioFrameObserver;
import io.agora.rtc2.IMediaRecorderCallback;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RecorderInfo;
import io.agora.rtc2.RtcConnection;
import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.RtcEngineEx;
import io.agora.rtc2.SimulcastStreamConfig;
import io.agora.rtc2.audio.AudioParams;
import io.agora.rtc2.video.VideoCanvas;
import io.agora.rtc2.video.VideoEncoderConfiguration;

import static io.agora.mediaplayer.Constants.MediaPlayerState.PLAYER_STATE_FAILED;
import static io.agora.mediaplayer.Constants.MediaPlayerState.PLAYER_STATE_IDLE;
import static io.agora.mediaplayer.Constants.MediaPlayerState.PLAYER_STATE_OPEN_COMPLETED;
import static io.agora.mediaplayer.Constants.MediaPlayerState.PLAYER_STATE_PAUSED;
import static io.agora.mediaplayer.Constants.MediaPlayerState.PLAYER_STATE_PLAYBACK_ALL_LOOPS_COMPLETED;
import static io.agora.mediaplayer.Constants.MediaPlayerState.PLAYER_STATE_PLAYBACK_COMPLETED;
import static io.agora.mediaplayer.Constants.MediaPlayerState.PLAYER_STATE_PLAYING;
import static io.agora.rtc2.Constants.AUDIO_EFFECT_OFF;
import static io.agora.rtc2.Constants.AUDIO_MIXING_REASON_ALL_LOOPS_COMPLETED;
import static io.agora.rtc2.Constants.AUDIO_MIXING_STATE_FAILED;
import static io.agora.rtc2.Constants.AUDIO_MIXING_STATE_PAUSED;
import static io.agora.rtc2.Constants.AUDIO_MIXING_STATE_PLAYING;
import static io.agora.rtc2.Constants.AUDIO_MIXING_STATE_STOPPED;
import static io.agora.rtc2.Constants.AUDIO_ROUTE_EARPIECE;
import static io.agora.rtc2.Constants.AUDIO_ROUTE_HEADSET;
import static io.agora.rtc2.Constants.AUDIO_ROUTE_HEADSETBLUETOOTH;
import static io.agora.rtc2.Constants.AUDIO_ROUTE_HEADSETNOMIC;
import static io.agora.rtc2.Constants.AUDIO_ROUTE_SPEAKERPHONE;
import static io.agora.rtc2.Constants.CONNECTION_CHANGED_JOIN_FAILED;
import static io.agora.rtc2.Constants.CONNECTION_CHANGED_JOIN_SUCCESS;
import static io.agora.rtc2.Constants.CONNECTION_STATE_CONNECTED;
import static io.agora.rtc2.Constants.CONNECTION_STATE_FAILED;
import static io.agora.rtc2.Constants.EAR_MONITORING_FILTER_NONE;
import static io.agora.rtc2.Constants.REMOTE_VIDEO_STATE_FAILED;
import static io.agora.rtc2.Constants.REMOTE_VIDEO_STATE_STARTING;
import static io.agora.rtc2.Constants.REMOTE_VIDEO_STATE_STOPPED;
import static io.agora.rtc2.Constants.ROOM_ACOUSTICS_3D_VOICE;
import static io.agora.rtc2.Constants.ROOM_ACOUSTICS_ETHEREAL;
import static io.agora.rtc2.Constants.ROOM_ACOUSTICS_KTV;
import static io.agora.rtc2.Constants.ROOM_ACOUSTICS_PHONOGRAPH;
import static io.agora.rtc2.Constants.ROOM_ACOUSTICS_SPACIAL;
import static io.agora.rtc2.Constants.ROOM_ACOUSTICS_STUDIO;
import static io.agora.rtc2.Constants.ROOM_ACOUSTICS_VIRTUAL_STEREO;
import static io.agora.rtc2.Constants.ROOM_ACOUSTICS_VOCAL_CONCERT;
import static io.agora.rtc2.video.VideoCanvas.RENDER_MODE_FIT;
import static io.agora.rtc2.video.VideoCanvas.RENDER_MODE_HIDDEN;
import static io.agora.rtc2.video.VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_15;
import static io.agora.rtc2.video.VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_ADAPTIVE;
import static io.agora.rtc2.video.VideoEncoderConfiguration.STANDARD_BITRATE;
import static io.agora.rtc2.video.VideoEncoderConfiguration.VD_180x180;
import static io.agora.rtc2.video.VideoEncoderConfiguration.VD_320x180;
import static io.agora.rtc2.video.VideoEncoderConfiguration.VD_360x360;
import static io.agora.rtc2.video.VideoEncoderConfiguration.VD_480x480;
import static io.agora.rtc2.video.VideoEncoderConfiguration.VD_640x480;

/**
 * Author   : zhuyanbo
 * Date     : 2020-03-31  19:43
 * Describe :
 */
public class AgoraChatRoomEngine implements IChatRoomEngine, IMediaPlayerObserver {
    private static final String TAG = "AgoraChatRoomEngine";
    private IRoomLiveStatusCallback iRoomLiveStatusCallback;
    private IVideoStateCallBack iVideoStateCallBack;
    private IAudioPlayerCallBack audioPlayerCallBack;
    private IRoomCallback iRoomCallback;
    private String token;
    private String publishToken;
    private String audioUniId;
    private boolean isMusicPlaying;
    private RtcEngineHandler iRtcEngineEventHandler;
    private IMusicPlayCallback iMusicPlayCallback;
    private IMusicMediaPlayerCallBack iMusicMediaPlayerCallBack;
    private IMediaPlayer mediaPlayer;
    private IMediaPlayer audioMediaPlayer;
    private String videoUrl;
    private String audioUrl;
    private boolean headSet;
    private boolean isEarEnable;
    private View remoteView;
    private String uid;
    Application context;
    private boolean openPublishAuth = false;
    private IAudioRecordCallBack audioRecordCallBack;
    private AudioRecordingParams audioRecordingParams;
    private int steamId;
    private int syncSteamId;
    private boolean isMusicPaused;
    private IEngineInitCallback initCallback;
    private TextureBufferHelper textureBufferHelper;
    private ByteBuffer alphaBuffer = null;
    private static final float[] matrix = new float[16];
    private boolean isLogin;
    private volatile boolean isPublishing;
    private ZegoLoginRoomCallback zegoLoginRoomCallback;
    private long mEglHandler = -1;
    private boolean setSource;
    //供外部使用
    private int outSteamId = -1;
    // 主线程
    private Handler handler = new Handler(Looper.getMainLooper());
    private MediaPlayerCustomDataProvider customDataProvider;
    private long currentAudioPosition;
    private boolean enableEarAudioEffect;
    private int audioDelay;
    private String chatType = SoulRtcEngine.Type.TYPE_BROADCAST;
    private IAgoraKTVSyncProcess syncProcessHelper;//KTV合唱同步
    private AgoraKTVChorusHelper ktvChorusHelper;//KTV合唱帮助类
    private int playerRole;
    private int playbackVolume = 100;
    private int recordingSignalVolume = 100;
    private String soPath;
    private String userId;
    private String appID;
    private boolean isDelayLocalSendPosition = true;
    private long audioDuration = 0;//

    private AgoraMediaRecorder agoraMediaRecorder = null;

    private IMediaRecorderCallback iMediaRecorderCallback;

    private RtcConnection rtcConnection;

    private boolean isAccompanyDelayPositionChange = false;
    private boolean isHit;
    private boolean isMultiRoom;

    private boolean isFollowSing; //当前是否为跟唱模式

    @Override
    public void initEngine(boolean isMultiRoom, Application context, String soPath, final String userId, String userName, String appID, byte[] appSign, boolean isTestEnv) {
        this.isMultiRoom = isMultiRoom;
        this.context = context;
        this.soPath = soPath;
        this.userId = userId;
        this.appID = appID;
        Matrix.setIdentityM(matrix, 0);
        SAaoraInstance.getInstance().initWorkerThread(context, Integer.parseInt(userId), appID, soPath);
        SLogKt.SLogApi.e("RoomChatEngineAgora", "initEngine initWorkerThread, soPath is = " + soPath + " appID = " + appID + " userId = " + userId);
        SAaoraInstance.getInstance().getEventHandler().addEventHandler(iRtcEngineEventHandler = new RtcEngineHandler() {
            @Override
            public void onFirstRemoteVideoDecoded(int uid, int width, int height, int elapsed) {

            }

            @Override
            public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
                isLogin = true;
                SLogKt.SLogApi.e("RoomChatEngineAgora", "onJoinChannelSuccess initWorkerThread, channel is = " + channel);
//                SAaoraInstance.getInstance().rtcEngine().enableVideo();
                if (SoulRtcEngine.Type.TYPE_COMMUNICATION.equals(chatType)) {
                    SLogKt.SLogApi.e("RoomChatEngineAgora", "onJoinChannelSuccess takeSeat");
                    takeSeat();
                }
                cn.soulapp.android.lib.media.ResultCode code = new cn.soulapp.android.lib.media.ResultCode(0, "");
                IRoomCallback iRoomCallback = AgoraChatRoomEngine.this.iRoomCallback;
                if (iRoomCallback != null) {
                    iRoomCallback.onLoginEventOccur(ZegoRoomLoginEvent.LOGIN_SUCCESS, 0, code);
                }
                SLogKt.SLogApi.e("RoomChatEngineAgora", "onJoinChannelSuccess onLoginEventOccur");
                muteSpeaker(speakerMute);
                ZegoLoginRoomCallback zegoLoginRoomCallback = AgoraChatRoomEngine.this.zegoLoginRoomCallback;

                if (zegoLoginRoomCallback != null) {
                    zegoLoginRoomCallback.onLoginRoom(new ResultCode(0, ""));
                    SLogKt.SLogApi.e("RoomChatEngineAgora", "onJoinChannelSuccess onLoginRoom");
                    AgoraChatRoomEngine.this.zegoLoginRoomCallback = null;
                }
            }

            @Override
            public void onConnectionStateChanged(int state) {
                super.onConnectionStateChanged(state);
            }

            @Override
            public void onConnectionStateChangedNew(int state, int reason) {
                super.onConnectionStateChangedNew(state, reason);
                if(state == CONNECTION_STATE_CONNECTED && reason == CONNECTION_CHANGED_JOIN_SUCCESS){
                    Map<String, Object> map = new HashMap<>();
                    map.put("hasToken", !TextUtils.isEmpty(token));
                    track("RTC_Join_Success", map);
                } else if (state == CONNECTION_STATE_FAILED && reason == CONNECTION_CHANGED_JOIN_FAILED) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("hasToken", !TextUtils.isEmpty(token));
                    track("RTC_Join_Fail", map);
                }
            }

            @Override
            public void onRejoinChannelSuccess(String channel, int uid, int elapsed) {
                super.onRejoinChannelSuccess(channel, uid, elapsed);
                isLogin = true;
                IRoomCallback iRoomCallback = AgoraChatRoomEngine.this.iRoomCallback;
                if (iRoomCallback != null) {
                    iRoomCallback.onRejoinChannelSuccess(channel, uid, elapsed);
                    SLogKt.SLogApi.e("RoomChatEngineAgora", "onRejoinChannelSuccess ok");
                }
            }

            @Override
            public void onConnectionLost() {
                super.onConnectionLost();
                IRoomCallback iRoomCallback = AgoraChatRoomEngine.this.iRoomCallback;
                if (iRoomCallback != null) {
                    iRoomCallback.onConnectionLost();
                    SLogKt.SLogApi.e("RoomChatEngineAgora", "onConnectionLost ok");
                }
            }

            @Override
            public void onNetWorkBad(int uid) {
                super.onNetWorkBad(uid);
                IRoomCallback iRoomCallback = AgoraChatRoomEngine.this.iRoomCallback;
                if (iRoomCallback != null) {
                    iRoomCallback.onNetWorkBad(String.valueOf(uid));
                    SLogKt.SLogApi.e("RoomChatEngineAgora", "onNetWorkBad uid = " + uid);
                }
            }


            @Override
            public void onRemoteAudioBad() {
                super.onRemoteAudioBad();
                IRoomCallback iRoomCallback = AgoraChatRoomEngine.this.iRoomCallback;
                if (iRoomCallback != null) {
                    iRoomCallback.onRemoteAudioBad();
                    SLogKt.SLogApi.e("RoomChatEngineAgora", "onRemoteAudioBad");
                }
            }


            @Override
            public void onRemoteVideoStateChanged(int uid, int state, int reason, int elapsed) {
                super.onRemoteVideoStateChanged(uid, state, reason, elapsed);
                SLogKt.SLogApi.e("RoomChatEngineAgora", "onRemoteVideoStateChanged state = " + state);
                IVideoStateCallBack iVideoStateCallBack = AgoraChatRoomEngine.this.iVideoStateCallBack;
                if (iVideoStateCallBack != null) {
                    if (state == REMOTE_VIDEO_STATE_STARTING) {
                        iVideoStateCallBack.onFirstRemoteVideoDecoded(String.valueOf(uid));
                        SLogKt.SLogApi.e("RoomChatEngineAgora", "onRemoteVideoStateChanged");
                    }
                }

                IRoomLiveStatusCallback roomLiveStatusCallback = iRoomLiveStatusCallback;
                if (roomLiveStatusCallback != null){
                    if (state == REMOTE_VIDEO_STATE_STOPPED || state == REMOTE_VIDEO_STATE_FAILED) {
                        roomLiveStatusCallback.onLiveQualityUpdate(String.valueOf(uid), "", 0.0, true);
                    }
                }
            }

            @Override
            public void onTokenPrivilegeWillExpire(String token) {
                super.onTokenPrivilegeWillExpire(token);
                AgoraKTVChorusHelper ktvChorusHelper = AgoraChatRoomEngine.this.ktvChorusHelper;
                if (token!=null && ktvChorusHelper != null && token.equals(ktvChorusHelper.getChorusToken())){
                    //合唱流token过期，直接不处理
                    return;
                }
                IRoomCallback iRoomCallback = AgoraChatRoomEngine.this.iRoomCallback;
                if (iRoomCallback != null) {
                    iRoomCallback.onTokenWillExpired();
                    SLogKt.SLogApi.e("RoomChatEngineAgora", "onTokenWillExpired");
                }
//                iRoomCallback.onRequestPublishToken(new IFetchTokenResultBlock() {
//                    @Override
//                    public void fetchTokenResult(String token) {
//                        SAaoraInstance.getInstance().rtcEngine().renewToken(token);
//                    }
//                });
            }

            @Override
            public void onStreamMessage(int uid, int streamId, byte[] data) {
                super.onStreamMessage(uid, streamId, data);
                try {
                    IRoomCallback iRoomCallback = AgoraChatRoomEngine.this.iRoomCallback;
                    if (iRoomCallback != null) {
                        iRoomCallback.onMessageReceived(data);
                    }
                    if (iRoomCallback != null) {
                        String dataStr = new String(data);
                        StreamMessage message = GsonUtils.jsonToEntity(dataStr, StreamMessage.class);
                        if (message != null) {
                            boolean isHandle = false;
                            try {
                                //多线程NPE避免
                                IAgoraKTVSyncProcess syncProcessHelper = AgoraChatRoomEngine.this.syncProcessHelper;
                                if (syncProcessHelper != null) {
                                    isHandle = syncProcessHelper.onStreamMessage(uid, streamId, message);
                                }
                            } catch (Throwable e) {
                                SLogKt.SLogApi.e(TAG, "syncProcessHelper onStreamMessage error:" + e.getMessage());
                                if (BuildConfig.DEBUG) {
                                    throw e;
                                }
                            }

                            if (isHandle) {
                                return;
                            }

                            try {
                                parseMediaSideInfo(message, dataStr);
                            } catch (Exception e) {
                                SLogKt.SLogApi.e(TAG, "parseMediaSideInfo onStreamMessage error:" + Log.getStackTraceString(e));
                            }
                        }
                    }
                }catch (Throwable e){
                    SLogKt.SLogApi.e(TAG, "onStreamMessage error:" + Log.getStackTraceString(e));
                    if (BuildConfig.DEBUG){
                        throw e;
                    }
                }
            }

            @Override
            public void onError(int errorCode) {
                IRoomCallback iRoomCallback = AgoraChatRoomEngine.this.iRoomCallback;
                if (iRoomCallback != null) {
                    iRoomCallback.onError(errorCode);
                    SLogKt.SLogApi.e("RoomChatEngineAgora", "errorCode errorCode = " + errorCode);
                }

                Map<String, Object> map = new HashMap<>();
                map.put("errorCode", errorCode+"");
                track("RTC_Error", map);
            }


            @Override
            public void onUserOffline(int uid, int reason) {
                IRoomCallback iRoomCallback = AgoraChatRoomEngine.this.iRoomCallback;
                if (iRoomCallback != null) {
                    iRoomCallback.onLiveUserLeave(String.valueOf(uid), "");
                    SLogKt.SLogApi.e("RoomChatEngineAgora", "onUserOffline uid = " + uid);
                }

                Map<String, Object> map = new HashMap<>();
                map.put("uid", uid+"");
                track("RTC_Remote_User_Left", map);
            }

            @Override
            public void onLocalAudioStats(IRtcEngineEventHandler.LocalAudioStats stats) {
                super.onLocalAudioStats(stats);
                audioDelay = stats.audioPlayoutDelay;
                IAgoraKTVSyncProcess syncProcessHelper = AgoraChatRoomEngine.this.syncProcessHelper;
                if (syncProcessHelper != null){
                    syncProcessHelper.setAudioDelay(audioDelay);
                }
                SLogKt.SLogApi.d("RoomChatEngineAgora", "audioPlayoutDelay="+audioDelay);
            }

            @Override
            public void onUserJoined(int uid, int elapsed) {
                IRoomCallback iRoomCallback = AgoraChatRoomEngine.this.iRoomCallback;
                if (iRoomCallback != null) {
                    iRoomCallback.onLiveUserJoin(String.valueOf(uid), "");
                    SLogKt.SLogApi.e("RoomChatEngineAgora", "onUserJoined uid = " + uid);
                }

                Map<String, Object> map = new HashMap<>();
                map.put("uid", uid+"");
                map.put("elapsed", elapsed+"");
                map.put("isHit", isHit);
                track("RTC_Remote_User_Join", map);
            }

            @Override
            public void onMusicPlayEnd() {
                super.onMusicPlayEnd();
            }

            @Override
            public void onAudioMixingStateChanged(int state, int errorCode) {
                super.onAudioMixingStateChanged(state, errorCode);
                SLogKt.SLogApi.e("RoomChatEngineAgora", "onAudioMixingStateChanged state = " + state + " isMusicPaused = " + isMusicPaused);
                IMusicPlayCallback iMusicPlayCallback = AgoraChatRoomEngine.this.iMusicPlayCallback;
                IMusicMediaPlayerCallBack iMusicMediaPlayerCallBack = AgoraChatRoomEngine.this.iMusicMediaPlayerCallBack;
                if (state == AUDIO_MIXING_STATE_PLAYING) {
                    if (isMusicPaused) {
                        if (null != iMusicPlayCallback) {
                            iMusicPlayCallback.onPlayResumed();
                        }
                        if (null != iMusicMediaPlayerCallBack) {
                            iMusicMediaPlayerCallBack.onPlayResumed();
                        }
                    } else {
                        if (null != iMusicPlayCallback) {
                            iMusicPlayCallback.onPlayStart();
                        }
                        if (null != iMusicMediaPlayerCallBack) {
                            iMusicMediaPlayerCallBack.onPlayStart();
                        }
                    }
                    isMusicPlaying = true;
                } else {
                    isMusicPlaying = false;
                }
                if (state == AUDIO_MIXING_STATE_STOPPED && errorCode == AUDIO_MIXING_REASON_ALL_LOOPS_COMPLETED) {
                    if (iMusicPlayCallback != null) {
                        iMusicPlayCallback.onPlayEnd();
                        onMusicPlayEnd();
                    }
                    if (null != iMusicMediaPlayerCallBack) {
                        iMusicMediaPlayerCallBack.onPlayEnd();
                        onMusicPlayEnd();
                    }
                } else if (state == AUDIO_MIXING_STATE_PAUSED) {
                    isMusicPaused = true;
                    if (iMusicPlayCallback != null) {
                        iMusicPlayCallback.onPlayPaused();
                    }
                    if (null != iMusicMediaPlayerCallBack) {
                        iMusicMediaPlayerCallBack.onPlayPaused();
                    }
                } else if (state == AUDIO_MIXING_STATE_FAILED) {
                    if (null != iMusicMediaPlayerCallBack) {
                        iMusicMediaPlayerCallBack.onPlayError(state, errorCode);
                    }
                }
            }

            @Override
            public void onAudioRouteChanged(int routing) {
                super.onAudioRouteChanged(routing);
                SLogKt.SLogApi.e("RoomChatEngineAgora", "onAudioRouteChanged routing = " + routing + " isEarEnable = " + isEarEnable);
                if (routing == AUDIO_ROUTE_SPEAKERPHONE
                        || routing == AUDIO_ROUTE_EARPIECE) {
                    //拔耳机
                    headSet = false;
                } else if (routing == AUDIO_ROUTE_HEADSET ||
                        routing == AUDIO_ROUTE_HEADSETNOMIC ||
                        routing == AUDIO_ROUTE_HEADSETBLUETOOTH) {
                    //插耳机
                    headSet = true;
                    if (isEarEnable) {
                        enableInEarMonitoring(true);
                    }
                }

                Map<String, Object> map = new HashMap<>();
                map.put("routing", routing+"");
                track("RTC_Audio_Route", map);
            }

            @Override
            public void onEffectPlayFinished() {
                super.onEffectPlayFinished();
                IZegoAudioPlayerCallback iZegoAudioPlayerCallback = AgoraChatRoomEngine.this.iZegoAudioPlayerCallback;
                if (iZegoAudioPlayerCallback != null) {
                    iZegoAudioPlayerCallback.onPlayEnd(0);
                    SLogKt.SLogApi.e("RoomChatEngineAgora", "onEffectPlayFinished routing");
                }
            }

            @Override
            public void onAudioQuality(int uid, int quality, short delay, short lost) {
                super.onAudioQuality(uid, quality, delay, lost);
                IRoomCallback iRoomCallback = AgoraChatRoomEngine.this.iRoomCallback;
                if (iRoomCallback != null) {
                    iRoomCallback.onAudioQuality(String.valueOf(uid), quality, delay, lost);
                    SLogKt.SLogApi.e("RoomChatEngineAgora", "onAudioQuality uid = " + uid + " quality = " + quality);
                }
            }

            @Override
            public void onRemoteVideoStats(IRtcEngineEventHandler.RemoteVideoStats stats) {
                super.onRemoteVideoStats(stats);
                IRoomLiveStatusCallback roomLiveStatusCallback = iRoomLiveStatusCallback;
                if (roomLiveStatusCallback != null){
                    roomLiveStatusCallback.onLiveQualityUpdate(String.valueOf(stats.uid), "", stats.receivedBitrate, false);
                }
            }

            @Override
            public void onAudioVolumeIndication(IRtcEngineEventHandler.AudioVolumeInfo[] speakerInfos, int totalVolume) {
                super.onAudioVolumeIndication(speakerInfos, totalVolume);
                if (speakerInfos == null) {
                    // quick and dirty fix for crash
                    // TODO should reset UI for no sound
                    return;
                }
                for (final IRtcEngineEventHandler.AudioVolumeInfo each : speakerInfos) {
                    final int peerVolume = each.volume;
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            IRoomLiveStatusCallback iRoomLiveStatusCallback = AgoraChatRoomEngine.this.iRoomLiveStatusCallback;
                            if (iRoomLiveStatusCallback != null) {
                                iRoomLiveStatusCallback.onGetSoundLevel(each.uid == 0 ? userId : String.valueOf(each.uid), "", peerVolume);
                            }
                        }
                    });
                }
            }
        });
        if (SAaoraInstance.getInstance().rtcEngine() == null) return;
//        registerVolumeChangeReceiver(context);
        IEngineInitCallback initCallback = AgoraChatRoomEngine.this.initCallback;
        if (initCallback != null) {
            initCallback.onEngineInit();
        }
        RtcEngine rtcEngine = SAaoraInstance.getInstance().rtcEngine();
        SLogKt.SLogApi.e("RoomChatEngineAgora", "SAaoraInstance.getInstance().rtcEngine() rtcEngine = " + rtcEngine);
        if (rtcEngine == null) return;
        if (isMultiRoom){
            //聊天室设为这个，默认值上麦后走的通话音量
            rtcEngine.setAudioScenario(Constants.AUDIO_SCENARIO_GAME_STREAMING);
        }
        rtcEngine.setDirectExternalAudioSource(true);
        rtcEngine.setRecordingAudioFrameParameters(48000, 1, 0, 480);
        rtcEngine.registerAudioFrameObserver(new IAudioFrameObserver() {
            @Override
            public boolean onRecordAudioFrame(String channelId, int type, int samplesPerChannel, int bytesPerSample, int channels, int samplesPerSec, ByteBuffer buffer, long renderTimeMs, int avsync_type) {
                AgoraKTVChorusHelper tempKtvChorusHelper = AgoraChatRoomEngine.this.ktvChorusHelper;
                //多线程可能导致NPE
                if (tempKtvChorusHelper != null) {
                    tempKtvChorusHelper.onRecordAudioFrame(buffer, renderTimeMs);
                }
                IAudioPlayerCallBack audioPlayerCallBack = AgoraChatRoomEngine.this.audioPlayerCallBack;
                if (null != audioPlayerCallBack) {
                    audioPlayerCallBack.onAudioPrep(buffer, samplesPerSec);
                }
                return true;
            }

            @Override
            public boolean onPlaybackAudioFrame(String channelId, int type, int samplesPerChannel, int bytesPerSample, int channels, int samplesPerSec, ByteBuffer buffer, long renderTimeMs, int avsync_type) {
                return false;
            }

            @Override
            public boolean onMixedAudioFrame(String channelId, int type, int samplesPerChannel, int bytesPerSample, int channels, int samplesPerSec, ByteBuffer buffer, long renderTimeMs, int avsync_type) {
                return false;
            }

            @Override
            public boolean onPlaybackAudioFrameBeforeMixing(String channelId, int userId, int type, int samplesPerChannel, int bytesPerSample, int channels, int samplesPerSec, ByteBuffer buffer, long renderTimeMs, int avsync_type) {
                return false;
            }

            @Override
            public boolean onPublishAudioFrame(String channelId, int type, int samplesPerChannel, int bytesPerSample, int channels, int samplesPerSec, ByteBuffer buffer, long renderTimeMs, int avsync_type) {
                return false;
            }

            @Override
            public int getObservedAudioFramePosition() {
                return 0;
            }

            @Override
            public AudioParams getRecordAudioParams() {
                return null;
            }

            @Override
            public AudioParams getPlaybackAudioParams() {
                return null;
            }

            @Override
            public AudioParams getMixedAudioParams() {
                return null;
            }

            @Override
            public AudioParams getPublishAudioParams() {
                return null;
            }
        });
        //audio profile=0表现为：48k单通道采样，audio发送码率是64kbps
        //audio profile=3表现为：48k双通道采样，audio发送码率是80kbps
        //audio profile=3音质会好一些
        //与IOS对齐
        rtcEngine.setAudioProfile(Constants.AUDIO_PROFILE_MUSIC_STANDARD_STEREO);
        rtcEngine.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING);
        rtcEngine.setClientRole(Constants.CLIENT_ROLE_AUDIENCE);
        rtcEngine.enableAudioVolumeIndication(1500, 3, false);
        muteSpeaker(false);
        rtcEngine.setParameters("{\"che.audio.specify.codec\":\"OPUSFB\"}");
        rtcEngine.setParameters("{\"rtc.enable_nasa2\":false}");  // 关闭nasa2，为了支持合唱低延迟
        rtcEngine.setParameters("{\"rtc.ntp_delay_drop_threshold\":1000}"); // ntp timeout 时间1s
        rtcEngine.setParameters("{\"rtc.video.enable_sync_render_ntp\": true}"); // 开启观众端ntp对齐
        rtcEngine.setParameters("{\"rtc.net.maxS2LDelay\": 800}"); // 观众端最大delay延迟
        rtcEngine.setParameters("{\"che.audio.max_mixed_participants\":8}"); // 最大支持8人合唱
        rtcEngine.setParameters("{\"che.audio.neteq.prebuffer\":true}");
        rtcEngine.setParameters("{\"che.audio.neteq.prebuffer_max_delay\":600}");
        rtcEngine.setParameters("{\"rtc.video.enable_sync_render_ntp_broadcast\":true}");//主播启用多端同步，静态设置
        rtcEngine.setParameters("{\"rtc.net.maxS2LDelayBroadcast\":500}");////主播的端到端延迟，静态设置。
        rtcEngine.setParameters("{\"che.audio.custom_bitrate\":48000}");;////主播的端到端延迟，静态设置。
        rtcEngine.setParameters("{\"rtc.video.send_alpha_data\":true}"); //虚拟人透明通道使用
        rtcEngine.setParameters("{\"rtc.video.alpha_data_codec_type\":3}");//虚拟人透明通道使用
        rtcEngine.setParameters("{\"che.video.android_bitrate_adjustment_type\":0}");//码率超发不要重置编码器

//        if (isTestEnv) {
//            rtcEngine.setParameters("{\"rtc.debug.enable\":true}");
//            rtcEngine.setParameters("{\"che.audio.frame_dump\":{\"location\":\"all\",\"action\":\"start\",\"max_size_bytes\":\"120000000\",\"uuid\":\"123456789\",\"duration\":\"1200000\"}}");
//        }


    }

    @Override
    public void initEngine(Application context, String type, String soPath, String userId, String userName, String appID, byte[] appSign, boolean isTestEnv) {
        initEngine(false, context, soPath, userId, userName, appID, appSign, isTestEnv);
    }

    private String businessType;

    @Override
    public void initEngine(Application context, String type, String soPath, String userId, String userName, String appID, byte[] appSign, boolean isTestEnv, String businessType) {
        this.businessType = businessType;
        initEngine(false, context, soPath, userId, userName, appID, appSign, isTestEnv);
    }

    @Override
    public void setEncodeParam() {

    }

    private void parseMediaSideInfo(StreamMessage message, String dataStr) {
        if (message == null) {
            return;
        }
        IAudioPlayerCallBack audioPlayerCallBack = AgoraChatRoomEngine.this.audioPlayerCallBack;

        //没有data，老版本格式
        if (TextUtils.isEmpty(message.data)) {
            if (!TextUtils.isEmpty(message.currentDuration)) {
                currentAudioPosition = Long.parseLong(message.currentDuration);
                if (null != audioPlayerCallBack) {
                    audioPlayerCallBack.onAudioPositionChanged("", currentAudioPosition, message.audioUniId, true);
                }
            } else {
                if (audioPlayerCallBack != null){
                    audioPlayerCallBack.onReceiveCustomMsg(dataStr);
                }
            }
            return;
        }

        String type = message.type;
        if (StreamMessage.TYPE_PLAY_STATUS.equals(type)) {
            //进度消息
            StreamMessage dataMessage = GsonUtils.jsonToEntity(message.data, StreamMessage.class);
            if (dataMessage != null && !TextUtils.isEmpty(dataMessage.currentDuration)) {
                //进度消息
                currentAudioPosition = Long.parseLong(dataMessage.currentDuration);
                if (null != audioPlayerCallBack) {
                    audioPlayerCallBack.onAudioPositionChanged("", currentAudioPosition, dataMessage.audioUniId, true);
                }
            }
        } else if (StreamMessage.TYPE_CUSTOM.equals(type)) {
            if (audioPlayerCallBack != null){
                audioPlayerCallBack.onReceiveCustomMsg(message.data);
            }
        }
    }

    //音准线算法需要单通道数据 而agora返回的是双通道 需要做相关处理
    private ByteBuffer channelDataConvert(ByteBuffer buffer, int samplesPerChannel, int bytesPerSample, int channels) {
        byte[] deinterleaved = new byte[samplesPerChannel * bytesPerSample];
        for (int i = 0; i < samplesPerChannel; i++) {
            int index = i * bytesPerSample;
            deinterleaved[index] = buffer.get(index * channels);
            deinterleaved[index + 1] = buffer.get(index * channels + 1);
        }
        ByteBuffer byteBuffer = ByteBuffer.allocate(deinterleaved.length);
        byteBuffer.put(deinterleaved);
        byteBuffer.flip();
        return byteBuffer;
    }

    @Override
    public void enterRoom(String roomId, String roomName, String userId, String userName) {
        SLogKt.SLogApi.e("RoomChatEngineAgora", "enterRoom roomId = " + roomId);
        if (chatType != null && chatType.equals(SoulRtcEngine.Type.TYPE_BROADCAST)) {
            IRoomCallback iRoomCallback = AgoraChatRoomEngine.this.iRoomCallback;
            if (iRoomCallback != null) {
                iRoomCallback.onRequestLoginToken();
            }
        }

        Map<String, Object> map = new HashMap<>();
        map.put("hasToken", !TextUtils.isEmpty(token));
        track(roomId, "RTC_Join_Start", map);

        int result = 0;
        if (chatType != null && chatType.equals(SoulRtcEngine.Type.TYPE_COMMUNICATION)) {
            result = SAaoraInstance.getInstance().joinChannel(roomId, token, SoulRtcEngine.Type.TYPE_BROADCAST);
        } else {
            result = SAaoraInstance.getInstance().joinRoomChannel(roomId, token, SoulRtcEngine.Type.TYPE_BROADCAST, isMultiRoom);
        }
        if (result != 0) {
            cn.soulapp.android.lib.media.ResultCode code = new cn.soulapp.android.lib.media.ResultCode(result, "");
            IRoomCallback iRoomCallback = AgoraChatRoomEngine.this.iRoomCallback;
            if (iRoomCallback != null) {
                iRoomCallback.onLoginEventOccur(ZegoRoomLoginEvent.LOGIN_FAILED, -1, code);
            }
        } else {
            isLogin = true;
        }
    }

    @Override
    public void leaveRoom() {
        boolean open = SConfiger.getBoolean("config_rtc_agora_leaveroom_swich", true);
        if(!open){
            exit();
            return;
        }
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                exit();
            }
        });
    }

    private void exit(){

        Map<String, Object> map = new HashMap<>();
        track("RTC_Local_User_Left", map);

        isLogin = false;
        haveAdjustVolum = false;
        ktvAudioPreset = AUDIO_EFFECT_OFF;
        stopMusic();
        SLogKt.SLogApi.e("RoomChatEngineAgora", "leaveRoom stopMusic");
        IMediaPlayer mediaPlayer = this.mediaPlayer;
        if (mediaPlayer != null) {
            mediaPlayer.unRegisterPlayerObserver(this);
            mediaPlayer.destroy();
            SLogKt.SLogApi.e("RoomChatEngineAgora", "leaveRoom mediaPlayer destroy");
            this.mediaPlayer = null;
        }
        IMediaPlayer audioMediaPlayer = this.audioMediaPlayer;
        if (audioMediaPlayer != null) {
            audioMediaPlayer.unRegisterPlayerObserver(this);
            audioMediaPlayer.destroy();
            SLogKt.SLogApi.e("RoomChatEngineAgora", "leaveRoom audioMediaPlayer destroy");
            this.audioMediaPlayer = null;
            audioDuration = 0;
        }
        IAgoraKTVSyncProcess syncProcessHelper = this.syncProcessHelper;
        if (syncProcessHelper != null) {
            syncProcessHelper.stop();
            this.syncProcessHelper = null;
        }
        AgoraKTVChorusHelper ktvChorusHelper = this.ktvChorusHelper;
        if (ktvChorusHelper != null) {
            ktvChorusHelper.stopPlay();
            this.ktvChorusHelper = null;
        }
        SAaoraInstance.getInstance().leaveChannel(null);
        SLogKt.SLogApi.e("RoomChatEngineAgora", "leaveRoom leaveChannel");
        SAaoraInstance.getInstance().deInitWorkerThread();
        SLogKt.SLogApi.e("RoomChatEngineAgora", "leaveRoom deInitWorkerThread");
        resetAudioState();
    }

    private boolean enableMic;
    private boolean isMute;

    @Override
    public void enableMic(final boolean enable) {
        if (isFollowSing && !enable) { //如果当前为跟唱模式，则不让关麦
            return;
        }
        boolean open = SConfiger.getBoolean("config_rtc_agora_leaveroom_swich", true);
        if(!open){
            mic(enable);
            return;
        }
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                mic(enable);
            }
        });
    }

    private void mic(boolean enable){

        Map<String, Object> map = new HashMap<>();
        map.put("state", enable ? "1" : "0");
        track("RTC_Mute", map);

        RtcEngineEx rtcEngineEx = SAaoraInstance.getInstance().rtcEngine();
        if (rtcEngineEx == null) return;
        SLogKt.SLogApi.d("sl_rtcEngine", "--enableMic--enable : " + enable);
        enableMic = enable;
        isMute = !enable;
        if (enable) {
            rtcEngineEx.setAudioEffectPreset(ktvAudioPreset);
        } else {
            rtcEngineEx.setAudioEffectPreset(AUDIO_EFFECT_OFF);
        }
//        ChannelMediaOptions options = SAaoraInstance.getInstance().getOptions();
//        if (options == null) return;
//        options.publishMicrophoneTrack = enable;
//        options.publishMediaPlayerAudioTrack = true;
//        SAaoraInstance.getInstance().updateOption(options);

        rtcEngineEx.muteRecordingSignal(!enable);

        AgoraKTVChorusHelper ktvChorusHelper = this.ktvChorusHelper;
        if (ktvChorusHelper != null) {
            ktvChorusHelper.enableMic(enable);
        }
    }

    private boolean speakerMute;

    @Override
    public void muteSpeaker(final boolean isMute) {
        boolean open = SConfiger.getBoolean("config_rtc_agora_leaveroom_swich", true);
        if(!open){
            speaker(isMute);
            return;
        }
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                speaker(isMute);
            }
        });
    }

    private void speaker(boolean isMute){
        speakerMute = isMute;
        if (SAaoraInstance.getInstance().rtcEngine() == null) return;
//        if (!isMute) {
//            if (isWiredHeadsetOn()) {
//                return;
//            }
//        }
        SLogKt.SLogApi.e("RoomChatEngineAgora", "muteSpeaker setDefaultAudioRoutetoSpeakerphoneWapper isMute = " + isMute);
        setDefaultAudioRoutetoSpeakerphoneWapper(!isMute);
    }

    public boolean isWiredHeadsetOn() {
        AudioManager localAudioManager = (AudioManager) (context.getSystemService(Context.AUDIO_SERVICE));
        return localAudioManager.isWiredHeadsetOn();
    }

    @Override
    public void takeSeat(String token) {
        RtcEngineEx rtcEngine = SAaoraInstance.getInstance().rtcEngine();
        if (rtcEngine == null){
            SLogKt.SLogApi.e(TAG, "takeSeat rtcEngine==null token="+token);
            return;
        }
        SLogKt.SLogApi.d("RoomChatEngineAgora", "--takeSeat--token : token = " + token);
        isPublishing = true;
        ChannelMediaOptions options = SAaoraInstance.getInstance().getOptions();
        if (options == null) return;
        options.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER;
        options.publishCameraTrack = false;
        options.token = token;
        SAaoraInstance.getInstance().updateOption(options);
        rtcEngine.enableLocalAudio(true);
        if (!isMute) {
            rtcEngine.muteRecordingSignal(false);
        }
        if (isFollowSing) {
            rtcEngine.muteLocalAudioStream(true);
        }

        Map<String, Object> map = new HashMap<>();
        map.put("state", "1");
        map.put("isHit", isHit ? "1" : "0");
        track("RTC_Mic_UpDown", map);
    }

    @Override
    public void startPushVideoFrame() {
        isPublishing = true;
        RtcEngineEx rtcEngineEx = SAaoraInstance.getInstance().rtcEngine();
        if (rtcEngineEx == null){
            SLogKt.SLogApi.e(TAG, "startPushVideoFrame rtcEngineEx==null");
            return;
        }

        ChannelMediaOptions options = new ChannelMediaOptions();
        options.publishCustomVideoTrack = true;
        rtcEngineEx.updateChannelMediaOptions(options);
    }

    @Override
    public void takeSeat() {
        RtcEngineEx rtcEngineEx = SAaoraInstance.getInstance().rtcEngine();
        if (rtcEngineEx == null){
            SLogKt.SLogApi.e(TAG, "takeSeat rtcEngineEx==null");
            return;
        }
        if (isStartRecordingDeviceTest) {  //上麦前需要退出跟唱的方法，不然会导致房主无声音
            isStartRecordingDeviceTest = false;
            rtcEngineEx.stopRecordingDeviceTest();
        }
        SLogKt.SLogApi.d("RoomChatEngineAgora", "--takeSeatWithoutToken---");
        isPublishing = true;
        ChannelMediaOptions options = SAaoraInstance.getInstance().getOptions();
        if (options == null) return;
        options.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER;
        options.publishCameraTrack = false;
        options.publishCustomVideoTrack = true;
        SAaoraInstance.getInstance().updateOption(options);
        rtcEngineEx.enableLocalAudio(true);
        rtcEngineEx.muteRecordingSignal(false);
        if (isFollowSing) { //跟唱模式不能闭麦
            rtcEngineEx.muteLocalAudioStream(true);
        }

        Map<String, Object> map = new HashMap<>();
        map.put("state", "1");
        map.put("isHit", isHit ? "1" : "0");
        track("RTC_Mic_UpDown", map);

    }

    @Override
    public void startAudioCapture() {
//        RtcEngineEx engineEx = SAaoraInstance.getInstance().rtcEngine();
//        if (null != engineEx) {
////            enableMic(true);
//            if (!enableMic) {
//                mic(true);
//            }
//            engineEx.muteLocalAudioStream(true);
////            engineEx.startRecordingDeviceTest(1500);
//        }
    }

    private boolean isStartRecordingDeviceTest = false;

    @Override
    public void handleFollowSing(boolean isOpen, boolean isSeatOn, boolean isMicOn, boolean isForce) {
        if(isFollowSing == isOpen && !isForce){
            return;
        }
        this.isFollowSing = isOpen;
        RtcEngineEx engineEx = SAaoraInstance.getInstance().rtcEngine();
        if (null == engineEx) {
            return;
        }
        if (isOpen) {
            if (!enableMic) {
                mic(true);
            }
            if (isSeatOn) {
                engineEx.muteLocalAudioStream(true);
            }
            if (!isSeatOn) {
                if (isStartRecordingDeviceTest) {
                    isStartRecordingDeviceTest = false;
                    engineEx.stopRecordingDeviceTest();
                }
                isStartRecordingDeviceTest = true;
                engineEx.startRecordingDeviceTest(1500);
            }
        } else {
            if (isSeatOn) {
                if (!isMicOn) {
                    mic(false);
                }
                engineEx.muteLocalAudioStream(false);
            } else {
                if (isStartRecordingDeviceTest) {
                    isStartRecordingDeviceTest = false;
                    engineEx.stopRecordingDeviceTest();
                }
            }
        }
    }

    @Override
    public void playMusic(IMusicPlayCallback callBack, String url) {
        this.iMusicPlayCallback = callBack;
        RtcEngineEx rtcEngine = SAaoraInstance.getInstance().rtcEngine();
        if (rtcEngine == null){
            SLogKt.SLogApi.e(TAG, "playMusic rtcEngine==null, url="+url);
            return;
        }
        ChannelMediaOptions options = SAaoraInstance.getInstance().getOptions();
        if (options == null) return;
        options.publishMediaPlayerAudioTrack = true;
        SAaoraInstance.getInstance().updateOption(options);
        rtcEngine.startAudioMixing(url, false, 1, 1);

        Map<String, Object> map = new HashMap<>();
        map.put("filePath",url);
        track("RTC_Play_Music", map);
    }

    @Override
    public void playMusic(IMusicPlayCallback callBack, String url, int loopCount) {
        this.iMusicPlayCallback = callBack;
        RtcEngineEx engineEx = SAaoraInstance.getInstance().rtcEngine();
        if (engineEx == null) {
            SLogKt.SLogApi.e(TAG, "playMusic rtcEngine==null url="+url +", loopCount="+loopCount);
            return;
        }
        engineEx.startAudioMixing(url, false, 1, 1);

        Map<String, Object> map = new HashMap<>();
        map.put("filePath",url);
        map.put("loopCount",loopCount+"");
        track("RTC_Play_Music", map);
    }

    @Override
    public void playMusic(IMusicMediaPlayerCallBack iMusicMediaPlayerCallBack, String url, boolean publish) {
        this.iMusicMediaPlayerCallBack = iMusicMediaPlayerCallBack;
        RtcEngineEx rtcEngine = SAaoraInstance.getInstance().rtcEngine();
        if (rtcEngine == null) return;
        ChannelMediaOptions options = SAaoraInstance.getInstance().getOptions();
        if (options == null) return;

        Map<String, Object> map = new HashMap<>();
        map.put("publish", publish ? "1" : "0");
        map.put("url", url);
        track("RTC_Play_Music", map);

        options.publishMediaPlayerAudioTrack = publish;
        SAaoraInstance.getInstance().updateOption(options);
        rtcEngine.startAudioMixing(url, !publish, 1, 1);
    }

    @Override
    public void pauseMusic() {
        RtcEngineEx rtcEngine = SAaoraInstance.getInstance().rtcEngine();
        if (rtcEngine == null) return;
        rtcEngine.pauseAudioMixing();

        Map<String, Object> map = new HashMap<>();
        track("RTC_Pause_Music", map);
    }

    @Override
    public void resumeMusic() {
        RtcEngineEx rtcEngine = SAaoraInstance.getInstance().rtcEngine();
        if (rtcEngine == null) return;
        rtcEngine.resumeAudioMixing();

        Map<String, Object> map = new HashMap<>();
        track("RTC_Resume_Music", map);
    }

    @Override
    public void setVolume(int volume) {
        RtcEngineEx rtcEngine = SAaoraInstance.getInstance().rtcEngine();
        if (rtcEngine == null) return;
        rtcEngine.adjustAudioMixingVolume(volume);
    }


    @Override
    public void stopLive(String token) {
        RtcEngineEx rtcEngine = SAaoraInstance.getInstance().rtcEngine();
        if (rtcEngine == null) return;
        SLogKt.SLogApi.d("RoomChatEngineAgora", "--leaveSeat--token : ");
        setSource = false;
        ChannelMediaOptions options = SAaoraInstance.getInstance().getOptions();
        if (options == null) return;
        options.clientRoleType = Constants.CLIENT_ROLE_AUDIENCE;
        options.publishCustomVideoTrack = false;
        options.token = token;
        SAaoraInstance.getInstance().updateOption(options);
        rtcEngine.enableLocalAudio(false);
//        SAaoraInstance.getInstance().rtcEngine().enableLocalVideo(false);

        Map<String, Object> map = new HashMap<>();
        map.put("role", "0");
        track("RTC_Role", map);
    }

    @Override
    public void stopPushVideoFrame() {
        isPublishing = false;
        RtcEngineEx rtcEngineEx = SAaoraInstance.getInstance().rtcEngine();
        if (rtcEngineEx == null){
            SLogKt.SLogApi.e(TAG, "startPushVideoFrame rtcEngineEx==null");
            return;
        }

        ChannelMediaOptions options = new ChannelMediaOptions();
        options.publishCustomVideoTrack = false;
        rtcEngineEx.updateChannelMediaOptions(options);
    }

    @Override
    public void stopLive() {
        RtcEngineEx rtcEngine = SAaoraInstance.getInstance().rtcEngine();
        if (rtcEngine == null) return;
//        isPublishing = false;
        SLogKt.SLogApi.d("RoomChatEngineAgora", "--leaveSeatWithoutToken---");
        setSource = false;
        alphaBuffer = null;
        if (textureBufferHelper != null) {
            textureBufferHelper.dispose();
            textureBufferHelper = null;
        }
        ChannelMediaOptions options = SAaoraInstance.getInstance().getOptions();
        if (options == null) return;
        options.clientRoleType = Constants.CLIENT_ROLE_AUDIENCE;
        options.publishCustomVideoTrack = false;
        SAaoraInstance.getInstance().updateOption(options);
        rtcEngine.enableLocalAudio(false);
//        SAaoraInstance.getInstance().rtcEngine().enableLocalVideo(false);

        Map<String, Object> map = new HashMap<>();
        map.put("role", "1");
        track("RTC_Role", map);
    }

    @Override
    public void setVideoDimension(SVideoDimension videoDimension) {
        RtcEngineEx rtcEngine = SAaoraInstance.getInstance().rtcEngine();
        if (rtcEngine == null) return;
        VideoEncoderConfiguration.VideoDimensions videoDimensions = VD_640x480;
        switch (videoDimension) {
            case SVD_180x180:
                videoDimensions = VD_180x180;
                break;
            case SVD_640x480:
                videoDimensions = VD_640x480;
                break;
            case SVD_360x360:
                videoDimensions = VD_360x360;
                break;
            case SVD_480x480:
                videoDimensions = VD_480x480;
                break;
            case SVD_320x180:
                videoDimensions = VD_320x180;
                break;
        }
        SAaoraInstance.getInstance().setVideoDimensions(videoDimensions);
        rtcEngine.setVideoEncoderConfiguration(new VideoEncoderConfiguration(
                videoDimensions,
                FRAME_RATE_FPS_15,
                STANDARD_BITRATE,
                ORIENTATION_MODE_ADAPTIVE));
    }

    private int ktvAudioPreset = AUDIO_EFFECT_OFF;

    @Override
    public void setAudioEffectPreset(int preset) {
        try {
            int audioPreset = AUDIO_EFFECT_OFF;
            switch (preset) {
                case Const.AudioPreset
                        .EffectPresetRoomAcousticsOFF:
                    audioPreset = AUDIO_EFFECT_OFF;
                    break;
                case Const.AudioPreset
                        .EffectPresetRoomAcousticsKTV:
                    audioPreset = ROOM_ACOUSTICS_KTV;
                    break;
                case Const.AudioPreset
                        .EffectPresetRommAcousVocalConcer:
                    audioPreset = ROOM_ACOUSTICS_VOCAL_CONCERT;
                    break;
                case Const.AudioPreset
                        .EffectPresetRommAcousStudio:
                    audioPreset = ROOM_ACOUSTICS_STUDIO;
                    break;
                case Const.AudioPreset
                        .EffectPresetRoomAcousPhonograph:
                    audioPreset = ROOM_ACOUSTICS_PHONOGRAPH;
                    break;
                case Const.AudioPreset
                        .EffectPresetRoomAcousVirtualStereo:
                    audioPreset = ROOM_ACOUSTICS_VIRTUAL_STEREO;
                    break;
                case Const.AudioPreset
                        .EffectPresetRoomAcousSpatial:
                    audioPreset = ROOM_ACOUSTICS_SPACIAL;
                    break;
                case Const.AudioPreset
                        .EffectPresetRoomAcousEthereal:
                    audioPreset = ROOM_ACOUSTICS_ETHEREAL;
                    break;
                case Const.AudioPreset
                        .EffectPresetRoomAcous3DVoice:
                    audioPreset = ROOM_ACOUSTICS_3D_VOICE;
                    break;
            }
            ktvAudioPreset = audioPreset;
            if (enableMic) {
                RtcEngineEx rtcEngine = SAaoraInstance.getInstance().rtcEngine();
                if (rtcEngine == null){
                    return;
                }
                rtcEngine.setAudioEffectPreset(audioPreset);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void removeMusicCallback(IMusicPlayCallback callback) {

    }

    @Override
    public boolean isPlaying() {
        return isMusicPlaying;
    }

    @Override
    public void addRoomCallBack(IRoomCallback iRoomCallback) {
        this.iRoomCallback = iRoomCallback;
    }

    @Override
    public void addRoomLiveStatusCallBack(IRoomLiveStatusCallback roomLiveStatusCallback) {
        this.iRoomLiveStatusCallback = roomLiveStatusCallback;
    }

    @Override
    public void addVideoStateCallBack(IVideoStateCallBack iRoomCallback) {
        this.iVideoStateCallBack = iRoomCallback;
    }

    @Override
    public void enableDTX(boolean enableDTX) {

    }

    @Override
    public void setAudioBitrate(int bitrate) {
    }

    /**
     * 获取设置过的码率
     *
     * @return 0表示没有设置过
     */
    public int getAudioBitrate() {
        return 0;
    }

    @Override
    public void setLoginToken(String loginToken) {
        this.token = loginToken;
    }

    @Override
    public void stopMusic() {
        RtcEngineEx rtcEngine = SAaoraInstance.getInstance().rtcEngine();
        if (rtcEngine == null) return;
        rtcEngine.stopAudioMixing();

        Map<String, Object> map = new HashMap<>();
        track("RTC_Stop_Music", map);
    }

    @Override
    public void setVolumeForUser(String userId, String userName, int volume) {
        RtcEngineEx rtcEngine = SAaoraInstance.getInstance().rtcEngine();
        if (rtcEngine == null) return;
        rtcEngine.adjustUserPlaybackSignalVolume(Integer.parseInt(userId), volume);
    }

    @Override
    public void setVolumeForUser(String userId, String userName, int volume, IUserIdDES userIdDES) {
        RtcEngineEx rtcEngine = SAaoraInstance.getInstance().rtcEngine();
        if (rtcEngine == null) return;
        if(userIdDES != null){
            int len = userId.length();
            //加密的uid才需要进行反解
            boolean need = false;
            for (int i = 0; i < len; i++) {
                if(!Character.isDigit(userId.charAt(i))){
                    need = true;
                    break;
                }
            }
            if(need){
                userId = userIdDES.decryption(userId);
            }
        }
        rtcEngine.adjustUserPlaybackSignalVolume(Integer.parseInt(userId), volume);
    }

    @Override
    public void setLocalVolume(int volume) {
        RtcEngineEx rtcEngine = SAaoraInstance.getInstance().rtcEngine();
        if (rtcEngine == null) return;
        haveAdjustVolum = true;
        recordingSignalVolume = volume * 2;
        rtcEngine.adjustRecordingSignalVolume(volume * 2);//本地需要发布的人声
        rtcEngine.setInEarMonitoringVolume(volume);//本地耳返音量
    }

    @Override
    public void setLocalVolume(int volume, int earMonitoringVolume) {
        RtcEngineEx rtcEngine = SAaoraInstance.getInstance().rtcEngine();
        if (rtcEngine == null) return;
        haveAdjustVolum = true;
        recordingSignalVolume = volume * 2;
        rtcEngine.adjustRecordingSignalVolume(volume);
        rtcEngine.setInEarMonitoringVolume(earMonitoringVolume);
    }

    @Override
    public synchronized void switchRoom(String t, final String roomID, String roomName, String uid, String userName, ZegoLoginRoomCallback loginRoomCallback) {
//        this.zegoLoginRoomCallback = loginRoomCallback;
//        SAaoraInstance.getInstance().leaveChannel(null);
//        initEngine(context,soPath,this.userId,"",appID,null,false);
//        iRoomCallback.onRequestLoginToken();
//        SAaoraInstance.getInstance().joinRoomChannel(roomName, token, SoulRtcEngine.Type.TYPE_BROADCAST);
        isLogin = false;
        haveAdjustVolum = false;
        stopMusic();
        SLogKt.SLogApi.d("RoomChatEngineAgora", "--switchRoom ---stopMusic");
        if (mediaPlayer != null) {
            mediaPlayer.unRegisterPlayerObserver(AgoraChatRoomEngine.this);
            mediaPlayer.destroy();
            SLogKt.SLogApi.d("RoomChatEngineAgora", "--switchRoom ---destroy");
            mediaPlayer = null;
        }
        leaveRoom();
        SLogKt.SLogApi.d("RoomChatEngineAgora", "--switchRoom ---leaveRoom");
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                initEngine(false, context, soPath, userId, "", appID, null, false);
                enterRoom(roomID, "", "", "");
            }
        }, 400);
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                while (true) {
//                    if (Const.isRtcDestroy) {
//                        new Handler().post(new Runnable() {
//                            @Override
//                            public void run() {
//
//                            }
//                        });
//                        break;
//                    }
//
//                }
//            }
//        }).start();
    }

    @Override
    public void switchRoomForGame(String token, String roomID, String roomName, String userId, String userName, ZegoLoginRoomCallback loginRoomCallback) {
        isLogin = false;
        haveAdjustVolum = false;
        stopMusic();
        SLogKt.SLogApi.d("RoomChatEngineAgora", "--switchRoomForGame ---stopMusic");
//        if (mediaPlayer != null) {
//            mediaPlayer.unRegisterPlayerObserver(AgoraChatRoomEngine.this);
//            mediaPlayer.destroy();
//            SLogKt.SLogApi.d("RoomChatEngineAgora", "--switchRoomForGame ---destroy");
//            mediaPlayer = null;
//        }



//        leaveRoom();
        SLogKt.SLogApi.d("RoomChatEngineAgora", "--switchRoomForGame ---leaveRoom");
        if (loginRoomCallback != null){

            loginRoomCallback.onLoginRoom(new ResultCode(0, ""));
        }
    }

    @Override
    public void enableMusicRepeat(boolean enable) {
    }

    IZegoAudioPlayerCallback iZegoAudioPlayerCallback;

    @Override
    public void playEffect(String path, int soundID, int loopCount, boolean publish, IZegoAudioPlayerCallback callback) {
        try {
            RtcEngineEx rtcEngine = SAaoraInstance.getInstance().rtcEngine();
            if (rtcEngine == null){
                return;
            }
            rtcEngine.getAudioEffectManager().stopAllEffects();
            rtcEngine.getAudioEffectManager().preloadEffect(soundID, path);
            iZegoAudioPlayerCallback = callback;
            rtcEngine.getAudioEffectManager().playEffect(soundID, path, 0, 1, 0.0, 100, publish);
            callback.onPlayEffect(soundID, 0);

            Map<String, Object> map = new HashMap<>();
            map.put("publish", publish ? "1" : "0");
            map.put("filePath", path);
            map.put("type", "IZegoAudioPlayerCallback");
            map.put("loopCount", loopCount+"");
            track("RTC_Play_Effect", map);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void renewToken(String token) {
        try {
            RtcEngineEx rtcEngine = SAaoraInstance.getInstance().rtcEngine();
            if (rtcEngine == null){
                return;
            }
            rtcEngine.renewToken(token);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void playEffect(String path, int soundID, int loopCount, boolean publish, IEffectPlayCallBack callback) {
        try {
            RtcEngineEx rtcEngine = SAaoraInstance.getInstance().rtcEngine();
            if (rtcEngine == null){
                return;
            }
            rtcEngine.getAudioEffectManager().stopAllEffects();
            rtcEngine.getAudioEffectManager().preloadEffect(soundID, path);
            iZegoAudioPlayerCallback = callback;
            if (loopCount > 0){
                loopCount = loopCount - 1;//api兼容3.x
            }

            Map<String, Object> map = new HashMap<>();
            map.put("publish", publish ? "1" : "0");
            map.put("filePath", path);
            map.put("loopCount", loopCount+"");
            track("RTC_Play_Effect", map);

            int result = rtcEngine.getAudioEffectManager().playEffect(soundID, path, loopCount, 1, 0.0, 100, publish);
            Log.e("-playEffect-", String.valueOf(result));
            if (result == 0) {
                callback.onPlayEffect(soundID, 0);
            } else {
                callback.onPlayError();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void playEffect(String path, int soundID, int loopCount, boolean publish, boolean interrupt, final IZegoAudioPlayerCallback callback) {
        try {
            RtcEngineEx rtcEngine = SAaoraInstance.getInstance().rtcEngine();
            if (rtcEngine == null){
                return;
            }
            if (interrupt) {
                rtcEngine.getAudioEffectManager().stopAllEffects();
            }
            rtcEngine.getAudioEffectManager().preloadEffect(soundID, path);
            iZegoAudioPlayerCallback = callback;
            rtcEngine.getAudioEffectManager().playEffect(soundID, path, 0, 1, 0.0, 100, publish);
            callback.onPlayEffect(soundID, 0);

            Map<String, Object> map = new HashMap<>();
            map.put("publish", publish ? "1" : "0");
            map.put("filePath", path);
            map.put("type", "IZegoAudioPlayerCallback");
            map.put("loopCount", loopCount+"");
            map.put("interrupt", interrupt+"");
            track("RTC_Play_Effect", map);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stopEffect(int soundID) {
        RtcEngineEx rtcEngine = SAaoraInstance.getInstance().rtcEngine();
        if (rtcEngine == null){
            return;
        }
        rtcEngine.stopEffect(soundID);

        Map<String, Object> map = new HashMap<>();
        map.put("type", "0");
        map.put("soundID", soundID+"");
        track("RTC_Stop_Effect", map);
    }

    @Override
    public void setEffectVolume(int soundID, int volume) {
        RtcEngineEx rtcEngine = SAaoraInstance.getInstance().rtcEngine();
        if (rtcEngine == null){
            return;
        }
        rtcEngine.setVolumeOfEffect(soundID, volume);
    }

    @Override
    public void setMusicVolume(int volume) {
    }

    @Override
    public void setSoundCycle(int timeInMS) {

    }

    @Override
    public void playVideo(String url, SurfaceView textureView) {

    }

    @Override
    public void playAudio(String url, String audioUniId) {
        this.audioUniId = audioUniId;
        currentAudioPosition = 0;
        videoUrl = null;
        initAudioMediaPlayer();

        audioDuration = 0;
        boolean changeSource =
                !url.equals(audioUrl);
        if (audioMediaPlayer.getState() == io.agora.mediaplayer.Constants.MediaPlayerState.PLAYER_STATE_PLAYING && changeSource) {
            audioMediaPlayer.stop();
        }
        audioUrl = url;
        // media player
        ChannelMediaOptions options = SAaoraInstance.getInstance().getOptions();
        if (options == null) return;
        options.publishMediaPlayerId = audioMediaPlayer.getMediaPlayerId();
        options.publishMediaPlayerAudioTrack = true;
        options.publishCustomVideoTrack = false;
        options.publishCameraTrack = false;
        options.publishMediaPlayerVideoTrack = false;
        SAaoraInstance.getInstance().updateOption(options);
        if (audioMediaPlayer.getState() != io.agora.mediaplayer.Constants.MediaPlayerState.PLAYER_STATE_PLAYING || changeSource) {
            if (audioMediaPlayer.getState() != io.agora.mediaplayer.Constants.MediaPlayerState.PLAYER_STATE_PAUSED) {
                audioMediaPlayer.open(url, 0);
            }
        }
    }

    private boolean haveAdjustVolum;

    @Override
    public void playEncryptAudio(String url, IMediaPlayerDecryptBlock fileReader) {
        isAccompanyDelayPositionChange = SConfiger.getBoolean("accompany_delay_position_change" ,false);
        customDataProvider = new MediaPlayerCustomDataProvider();
        videoUrl = null;
        initAudioMediaPlayer();
        if (!haveAdjustVolum) {
            setLocalVolume(100);
            setVideoVolume(50);
        }

        audioDuration = 0;
        boolean changeSource =
                !url.equals(audioUrl);
        if (null != audioMediaPlayer && audioMediaPlayer.getState() == io.agora.mediaplayer.Constants.MediaPlayerState.PLAYER_STATE_PLAYING && changeSource) {
            audioMediaPlayer.stop();
        }
        audioUrl = url;
        customDataProvider.setUrl(url);
        customDataProvider.setMediaPlayerFileReader(fileReader);
        // media player
        ChannelMediaOptions options = SAaoraInstance.getInstance().getOptions();
        if (options == null) return;
        options.publishMediaPlayerId = audioMediaPlayer.getMediaPlayerId();
        options.publishMediaPlayerAudioTrack = true;
        options.publishCustomVideoTrack = false;
        options.publishCameraTrack = false;
        options.publishMediaPlayerVideoTrack = false;
        SAaoraInstance.getInstance().updateOption(options);
        if (null != audioMediaPlayer && audioMediaPlayer.getState() != io.agora.mediaplayer.Constants.MediaPlayerState.PLAYER_STATE_PLAYING || changeSource) {
            if (audioMediaPlayer.getState() != io.agora.mediaplayer.Constants.MediaPlayerState.PLAYER_STATE_PAUSED) {
                audioMediaPlayer.openWithCustomSource(0, customDataProvider);
            }
        }
    }

    @Override
    public void playKTVEncryptAudio(PlayKTVParams params) {
        String url = params.url;
        int role = params.role;
        isAccompanyDelayPositionChange = SConfiger.getBoolean("accompany_delay_position_change" ,false);
        this.audioUniId = params.audioUniId;
        isDelayLocalSendPosition = true;
        playerRole = role;
        videoUrl = null;
        initAudioMediaPlayer();
        if (!haveAdjustVolum) {
            setLocalVolume(100);
            if (role == Const.KTV_ROLE_ACCOMPANY_SINGER) {
                //伴唱
                audioMediaPlayer.adjustPlayoutVolume(25);
            } else {
                setVideoVolume(50);
            }
        }

        audioDuration = 0;
        boolean changeSource =
                !url.equals(audioUrl);
        if (audioMediaPlayer.getState() == io.agora.mediaplayer.Constants.MediaPlayerState.PLAYER_STATE_PLAYING && changeSource) {
            audioMediaPlayer.stop();
        }
        audioUrl = url;
        if (params.playType == Const.KtvAudioPlayType.playTypeEncryption) {
            customDataProvider = new MediaPlayerCustomDataProvider();
            customDataProvider.setUrl(url);
            customDataProvider.setMediaPlayerFileReader(params.fileReader);
        }

        RtcEngineEx rtcEngine = SAaoraInstance.getInstance().rtcEngine();
        if (rtcEngine == null){
            SLogKt.SLogApi.e(TAG, "playKTVEncryptAudio rtcEngine==null");
            return;
        }
        AgroaEngineConfig config = SAaoraInstance.getInstance().getConfig();

        AgoraKTVConfig ktvConfig = new AgoraKTVConfig();
        ktvConfig.role = role;
        ktvConfig.rtcEngine = rtcEngine;
        ktvConfig.mediaPlayer = audioMediaPlayer;
        ktvConfig.agroaEngineConfig = config;
        ktvConfig.chorusToken = params.chorusToken;
        ktvConfig.chorusChannelId = params.chorusChannelId;
        ktvConfig.leaderUid = Integer.parseInt(params.curSingerUid);
        ktvConfig.syncSteamId = syncSteamId;
        ktvConfig.playbackSignalVolume = params.playbackSignalVolume;

        if (ktvChorusHelper != null) {
            ktvChorusHelper.stopPlay();
        }

        ktvChorusHelper = new AgoraKTVChorusHelper(ktvConfig);
        ktvChorusHelper.enableMic(enableMic);
        ktvChorusHelper.startPlay();

        if (syncProcessHelper != null) {
            syncProcessHelper.stop();
        }
//        syncProcessHelper = new AgoraKTVSyncProcessHelper(ktvConfig);
        syncProcessHelper = new AgoraKTVSyncProcessNTPHelper(ktvConfig);
        if (role == Const.KTV_ROLE_ACCOMPANY_SINGER) {
            syncProcessHelper.startSyncProcess();
        }

        if (audioMediaPlayer.getState() != io.agora.mediaplayer.Constants.MediaPlayerState.PLAYER_STATE_PLAYING || changeSource) {
            if (audioMediaPlayer.getState() != io.agora.mediaplayer.Constants.MediaPlayerState.PLAYER_STATE_PAUSED) {
                if (params.playType == Const.KtvAudioPlayType.playTypeEncryption) {
                    audioMediaPlayer.openWithCustomSource(0, customDataProvider);
                } else {
                    audioMediaPlayer.open(url, 0);
                }
            }
        }
    }

    @Override
    public void setKTVUserRole(int role) {

    }

    @Override
    public void setPlaybackSignalVolume(int playbackSignalVolume) {
        RtcEngineEx rtcEngine = SAaoraInstance.getInstance().rtcEngine();
        if (rtcEngine == null) {
            return;
        }
        if (playbackSignalVolume >= 100) {
            rtcEngine.adjustPlaybackSignalVolume(100);
            playbackVolume = 100;
        } else {
            rtcEngine.adjustPlaybackSignalVolume(playbackSignalVolume);
            playbackVolume = playbackSignalVolume;
        }
    }

    @Override
    public void setPlayQualityMonitorCycle(long timeInMS) {

    }

    @Override
    public void enableUserLiveQualityUpdate(boolean enable) {

    }

    @Override
    public void playVideo(String url, TextureView textureView) {
        RtcEngineEx rtcEngine = SAaoraInstance.getInstance().rtcEngine();
        if (rtcEngine == null) {
            return;
        }
        audioUrl = null;
        initMediaPlayer();
        boolean changeSource =
                !url.equals(videoUrl);
        if (mediaPlayer.getState() == io.agora.mediaplayer.Constants.MediaPlayerState.PLAYER_STATE_PLAYING && changeSource) {
            mediaPlayer.stop();
            SLogKt.SLogApi.d("RoomChatEngineAgora", "--playVideo ---stop");
        }
        cleanVideoView();
        videoUrl = url;
        // media player
//        SAaoraInstance.getInstance().rtcEngine().enableVideo();
        ChannelMediaOptions options = SAaoraInstance.getInstance().getOptions();
        if (options == null) return;
        options.publishMediaPlayerId = mediaPlayer.getMediaPlayerId();
        options.publishMediaPlayerAudioTrack = true;
        options.publishCustomVideoTrack = false;
        options.publishCameraTrack = false;
        options.publishMediaPlayerVideoTrack = true;
        SAaoraInstance.getInstance().updateOption(options);
        rtcEngine.setVideoEncoderConfiguration(new VideoEncoderConfiguration(
                VD_640x480,
                FRAME_RATE_FPS_15,
                STANDARD_BITRATE,
                ORIENTATION_MODE_ADAPTIVE));
        VideoCanvas videoCanvas = new VideoCanvas(textureView, Constants.RENDER_MODE_FIT, Constants.VIDEO_MIRROR_MODE_AUTO,
                Constants.VIDEO_SOURCE_MEDIA_PLAYER, mediaPlayer.getMediaPlayerId(), 0);
        rtcEngine.setupLocalVideo(videoCanvas);
        if (mediaPlayer.getState() != io.agora.mediaplayer.Constants.MediaPlayerState.PLAYER_STATE_PLAYING || changeSource) {
            if (mediaPlayer.getState() != io.agora.mediaplayer.Constants.MediaPlayerState.PLAYER_STATE_PAUSED) {
                mediaPlayer.open(url, 0);
            }
        }
    }

    @Override
    public void stopVideo() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            cleanVideoView();
            ChannelMediaOptions options = SAaoraInstance.getInstance().getOptions();
            if (options == null) return;
            options.publishMediaPlayerAudioTrack = false;
            options.publishMediaPlayerVideoTrack = false;
            SAaoraInstance.getInstance().updateOption(options);
        }
    }

    @Override
    public void stopAudio() {
        resetAudioState();
        if (audioMediaPlayer != null) {
            audioMediaPlayer.stop();
            audioDuration = 0;
            ChannelMediaOptions options = SAaoraInstance.getInstance().getOptions();
            if (options == null) return;
            options.publishMediaPlayerAudioTrack = false;
            options.publishMediaPlayerVideoTrack = false;
//            ktvAudioPreset = AUDIO_EFFECT_OFF;
            SAaoraInstance.getInstance().updateOption(options);
        }

        if (syncProcessHelper != null) {
            syncProcessHelper.stop();
            syncProcessHelper = null;
        }
        if (ktvChorusHelper != null) {
            ktvChorusHelper.stopPlay();
            ktvChorusHelper = null;
        }
    }

    //重置音频相关状态
    private void resetAudioState(){
        playerRole = 0;
        audioUniId = "";
    }

    private void cleanVideoView() {
        RtcEngineEx rtcEngine = SAaoraInstance.getInstance().rtcEngine();
        if (rtcEngine == null){
            return;
        }
        //clean
        rtcEngine.setupLocalVideo(new VideoCanvas(null, Constants.RENDER_MODE_FIT, Constants.VIDEO_MIRROR_MODE_AUTO,
                Constants.VIDEO_SOURCE_MEDIA_PLAYER, mediaPlayer.getMediaPlayerId(), 0));
        if (!TextUtils.isEmpty(uid)) {
            rtcEngine.setupRemoteVideo(new VideoCanvas(null, RENDER_MODE_FIT, Integer.parseInt(uid)));
        }
    }

    @Override
    public void pauseAudio() {
        if (audioMediaPlayer != null) {
            audioMediaPlayer.pause();
        }
    }

    @Override
    public void pauseVideo() {
        if (mediaPlayer != null) {
            mediaPlayer.pause();
        }
    }

    @Override
    public void resumeVideo() {
        if (mediaPlayer != null) {
            mediaPlayer.resume();
        }
    }

    @Override
    public void resumeAudio() {
        if (audioMediaPlayer != null) {
            audioMediaPlayer.resume();
        }
    }

    @Override
    public void setupRemoteVideoView(String uid, TextureView textureView, boolean withAlpha) {
        this.uid = uid;
        ChannelMediaOptions options = SAaoraInstance.getInstance().getOptions();
        if (options == null) return;

        Map<String, Object> map = new HashMap<>();
        map.put("state", "1");
        map.put("type", "TextureView");
        track("RTC_Remote_Render", map);

        options.autoSubscribeVideo = true;
        SAaoraInstance.getInstance().updateOption(options);
        if (!TextUtils.isEmpty(uid)) {
            RtcEngineEx rtcEngine = SAaoraInstance.getInstance().rtcEngine();
            if (rtcEngine == null){
                return;
            }
            rtcEngine.setupRemoteVideo(new VideoCanvas(null, RENDER_MODE_HIDDEN, Integer.parseInt(uid)));
            VideoCanvas canvas = new VideoCanvas(textureView, RENDER_MODE_HIDDEN, Integer.parseInt(uid));
            if (withAlpha) {
                textureView.setOpaque(false);
                canvas.enableAlphaMask = true;
            }
            rtcEngine.setupRemoteVideo(canvas);
        }
    }

    @Override
    public void setupRemoteVideoView(RemoteViewParams params) {
        //暂时只实现了火山
    }

    @Override
    public void destroyRemoteVideoView() {

    }

    @Override
    public void setupRemoteVideoView(String uid, SurfaceView textureView) {
        this.uid = uid;
        ChannelMediaOptions options = SAaoraInstance.getInstance().getOptions();
        if (options == null) return;

        Map<String, Object> map = new HashMap<>();
        map.put("state", "1");
        map.put("type", "SurfaceView");
        track("RTC_Remote_Render", map);

        options.autoSubscribeVideo = true;
        SAaoraInstance.getInstance().updateOption(options);
        if (!TextUtils.isEmpty(uid)) {
            RtcEngineEx rtcEngine = SAaoraInstance.getInstance().rtcEngine();
            if (rtcEngine == null){
                return;
            }
            rtcEngine.setupRemoteVideo(new VideoCanvas(textureView, RENDER_MODE_HIDDEN, Integer.parseInt(uid)));
        }
    }

    @Override
    public boolean isVideoPlaying() {
        return mediaPlayer != null && mediaPlayer.getState() == io.agora.mediaplayer.Constants.MediaPlayerState.PLAYER_STATE_PLAYING;
    }

    @Override
    public boolean isAudioPlaying() {
        return audioMediaPlayer != null && audioMediaPlayer.getState() == io.agora.mediaplayer.Constants.MediaPlayerState.PLAYER_STATE_PLAYING;
    }

    @Override
    public boolean isVideoPlaying(String url) {
        return isVideoPlaying() && url.equals(videoUrl);
    }

    @Override
    public String getCurrentVideoUrl() {
        return videoUrl;
    }

    @Override
    public void setVideoVolume(int volume) {
        if (mediaPlayer != null) {
            mediaPlayer.adjustPlayoutVolume(volume);
            mediaPlayer.adjustPublishSignalVolume(volume);
        }
        if (audioMediaPlayer != null) {
            haveAdjustVolum = true;
            audioMediaPlayer.adjustPlayoutVolume(volume);//本地播放的音量
            audioMediaPlayer.adjustPublishSignalVolume(volume / 2);//发到远端的音量
        }
    }

    private int playoutVolume;
    private int publishSignalVolume;

    @Override
    public void setVideoVolume(int playoutVolume, int publishSignalVolume) {
        if (mediaPlayer != null) {
            mediaPlayer.adjustPlayoutVolume(playoutVolume);
            mediaPlayer.adjustPublishSignalVolume(publishSignalVolume);
        }
        if (audioMediaPlayer != null) {
            audioMediaPlayer.adjustPlayoutVolume(playoutVolume);
            audioMediaPlayer.adjustPublishSignalVolume(publishSignalVolume);
        }
        this.playoutVolume = playoutVolume;
        this.publishSignalVolume = publishSignalVolume;
        haveAdjustVolum = true;
    }

    @Override
    public void enableInEarMonitoring(boolean enable) {
        try {
            isEarEnable = enable;
            RtcEngineEx rtcEngine = SAaoraInstance.getInstance().rtcEngine();
            if (rtcEngine == null){
                return;
            }
            if (!enable) {
                if (!isWiredHeadsetOn()) {
                    setDefaultAudioRoutetoSpeakerphoneWapper(true);
                }
                rtcEngine.enableInEarMonitoring(false, EAR_MONITORING_FILTER_NONE);
            } else {
                SLogKt.SLogApi.d("RoomChatEngineAgora", "--enableInEarMonitoring ---headSet = " + headSet);
                if (headSet) {
                    setDefaultAudioRoutetoSpeakerphoneWapper(false);
                    rtcEngine.enableInEarMonitoring(true, EAR_MONITORING_FILTER_NONE);
//                    if (enableEarAudioEffect) {
//                        rtcEngine.enableInEarMonitoring(true, EAR_MONITORING_FILTER_BUILT_IN_AUDIO_FILTERS);
//                    } else {
//                        rtcEngine.enableInEarMonitoring(true, EAR_MONITORING_FILTER_BUILT_IN_AUDIO_FILTERS | EAR_MONITORING_FILTER_NOISE_SUPPRESSION);
//                    }
                }
            }
        } catch (Exception e) {
            SLogKt.SLogApi.d("RoomChatEngineAgora", "--enableInEarMonitoring ---e = " + e.toString());
        }
    }

    @Override
    public String getRoomId() {
        AgroaEngineConfig config = SAaoraInstance.getInstance().getConfig();
        if (config == null) {
            return null;
        }
        return config.channel;
    }

    @Override
    public void onPlayerStateChanged(io.agora.mediaplayer.Constants.MediaPlayerState mediaPlayerState, io.agora.mediaplayer.Constants.MediaPlayerError mediaPlayerError) {
        IRoomCallback iRoomCallback = AgoraChatRoomEngine.this.iRoomCallback;
        if (videoUrl != null) {
            if (mediaPlayerState.equals(PLAYER_STATE_OPEN_COMPLETED)) {
                audioDuration = audioMediaPlayer.getDuration();
                mediaPlayer.play();
                if (audioTrack >= 0){
                    selectAudioTrack(audioTrack);
                }
                if (iRoomCallback != null) {
                    iRoomCallback.onLocalVideoStateChanged(SLMediaPlayerState.PLAYER_STATE_START);
                }
            } else if (mediaPlayerState.equals(PLAYER_STATE_PLAYBACK_COMPLETED) || mediaPlayerState.equals(PLAYER_STATE_PLAYBACK_ALL_LOOPS_COMPLETED)) {
                videoUrl = null;
                if (iRoomCallback != null) {
                    iRoomCallback.onLocalVideoStateChanged(SLMediaPlayerState.PLAYER_STATE_COMPLETE);
                }
            } else if (mediaPlayerState.equals(PLAYER_STATE_FAILED)) {
                videoUrl = null;
                if (iRoomCallback != null) {
                    iRoomCallback.onLocalVideoStateChanged(SLMediaPlayerState.PLAYER_STATE_FAILED);
                }
            } else if (mediaPlayerState.equals(PLAYER_STATE_IDLE)) {
                if (iRoomCallback != null) {
                    iRoomCallback.onLocalVideoStateChanged(SLMediaPlayerState.PLAYER_STATE_STOP);
                }
            }
        } else if (audioUrl != null) {
            if (mediaPlayerState.equals(PLAYER_STATE_OPEN_COMPLETED)) {
                audioDuration = audioMediaPlayer.getDuration();
                if (playerRole != Const.KTV_ROLE_ACCOMPANY_SINGER) {
                    audioMediaPlayer.play();
                }
                if (audioTrack >= 0){
                    selectAudioTrack(audioTrack);
                }
                if (iRoomCallback != null) {
                    iRoomCallback.onLocalAudioStateChanged(SLMediaPlayerState.PLAYER_STATE_START);
                }
            } else if (mediaPlayerState.equals(PLAYER_STATE_PLAYBACK_COMPLETED) || mediaPlayerState.equals(PLAYER_STATE_PLAYBACK_ALL_LOOPS_COMPLETED)) {
                audioUrl = null;
                if (iRoomCallback != null) {
                    iRoomCallback.onLocalAudioStateChanged(SLMediaPlayerState.PLAYER_STATE_COMPLETE);
                }
            } else if (mediaPlayerState.equals(PLAYER_STATE_FAILED)) {
                audioUrl = null;
                if (iRoomCallback != null) {
                    iRoomCallback.onLocalAudioStateChanged(SLMediaPlayerState.PLAYER_STATE_FAILED);
                }
            } else if (mediaPlayerState.equals(PLAYER_STATE_IDLE)) {
                if (iRoomCallback != null) {
                    iRoomCallback.onLocalAudioStateChanged(SLMediaPlayerState.PLAYER_STATE_STOP);
                }
            } else if (mediaPlayerState.equals(PLAYER_STATE_PAUSED)) {
                if (iRoomCallback != null) {
                    iRoomCallback.onLocalAudioStateChanged(SLMediaPlayerState.PLAYER_STATE_PAUSED);
                }
            } else if (mediaPlayerState.equals(PLAYER_STATE_PLAYING)) {
                if (iRoomCallback != null) {
                    iRoomCallback.onLocalAudioStateChanged(SLMediaPlayerState.PLAYER_STATE_PLAYING);
                }
            }
        }

        if (mediaPlayerState.equals(PLAYER_STATE_PLAYING)) {
            //多线程NPE避免
            IAgoraKTVSyncProcess syncProcessHelper = this.syncProcessHelper;
            if (syncProcessHelper != null) {
                syncProcessHelper.onStartPlay();
            }
            AgoraKTVChorusHelper ktvChorusHelper = this.ktvChorusHelper;
            if (ktvChorusHelper != null) {
                ktvChorusHelper.onStartPlay();
            }
        }

        RtcEngineEx rtcEngineEx = SAaoraInstance.getInstance().rtcEngine();

        //多线程NPE避免
        IAgoraKTVSyncProcess syncProcessHelper = this.syncProcessHelper;

        if (rtcEngineEx != null && playerRole == Const.KTV_ROLE_LEADER_SINGER && audioMediaPlayer != null) {
            StreamMessage streamMessage = new StreamMessage();
            streamMessage.type = StreamMessage.TYPE_PLAY_STATUS;
            if (syncProcessHelper != null) {
                streamMessage.currentTimeStamp = String.valueOf(syncProcessHelper.getNtpTime());
            }
            streamMessage.currentDuration = String.valueOf(audioMediaPlayer.getPlayPosition());
            if (audioDuration <= 0) {
                audioDuration = audioMediaPlayer.getDuration();
            }
            streamMessage.audioDelay = String.valueOf(audioDelay);
            streamMessage.audioUniId = audioUniId;
            streamMessage.duration = String.valueOf(audioDuration);
            streamMessage.playerState = String.valueOf(io.agora.mediaplayer.Constants.MediaPlayerState.getValue(mediaPlayerState));
            String data = GsonUtils.entityToJson(streamMessage);
            if (data != null) {
                rtcEngineEx.sendStreamMessage(steamId, data.getBytes(StandardCharsets.UTF_8));
            }
        }


        if (syncProcessHelper != null){
            syncProcessHelper.onPlayerStateChanged(mediaPlayerState, mediaPlayerError);
        }

    }

    @Override
    public void onPositionChanged(long l) {
        //多线程NPE避免
        IAgoraKTVSyncProcess syncProcessHelper = this.syncProcessHelper;
        if (syncProcessHelper != null){
            syncProcessHelper.onPositionChanged(l);
        }
        IAudioPlayerCallBack audioPlayerCallBack = AgoraChatRoomEngine.this.audioPlayerCallBack;
        if (!isDelayLocalSendPosition || playerRole == Const.KTV_ROLE_LEADER_SINGER
                || (playerRole == Const.KTV_ROLE_ACCOMPANY_SINGER
                && (!isAccompanyDelayPositionChange || Math.abs(l - currentAudioPosition) < 1000))) {
            isDelayLocalSendPosition = false;
            currentAudioPosition = l;
            if (null != audioPlayerCallBack) {
                audioPlayerCallBack.onAudioPositionChanged(uid, currentAudioPosition, audioUniId, false);
            }
            RtcEngineEx rtcEngine = SAaoraInstance.getInstance().rtcEngine();
            if (rtcEngine!=null && playerRole == Const.KTV_ROLE_LEADER_SINGER) {
                StreamMessage streamMessage = new StreamMessage();
                streamMessage.type = StreamMessage.TYPE_PLAY_STATUS;
                if (syncProcessHelper != null) {
                    streamMessage.currentTimeStamp = String.valueOf(syncProcessHelper.getNtpTime());
                }
                streamMessage.currentDuration = String.valueOf(l - audioDelay);
                streamMessage.audioDelay = String.valueOf(audioDelay);
                if (audioDuration <= 0) {
                    audioDuration = audioMediaPlayer.getDuration();
                    SLogKt.SLogApi.e(TAG, "onPositionChanged getDuration:"+audioDuration);
                }
                streamMessage.audioUniId = audioUniId;
                streamMessage.duration = String.valueOf(audioDuration);
                io.agora.mediaplayer.Constants.MediaPlayerState mediaPlayerState = null;
                if (syncProcessHelper != null){
                    mediaPlayerState = syncProcessHelper.getMediaPlayerState();
                }
                if (mediaPlayerState == null){
                    mediaPlayerState = audioMediaPlayer.getState();
                }
                streamMessage.playerState = String.valueOf(io.agora.mediaplayer.Constants.MediaPlayerState.getValue(mediaPlayerState));
                String data = GsonUtils.entityToJson(streamMessage);
                if (data != null)
                    rtcEngine.sendStreamMessage(syncSteamId, data.getBytes(StandardCharsets.UTF_8));
            }
        }

    }

    @Override
    public void onPlayerEvent(io.agora.mediaplayer.Constants.MediaPlayerEvent mediaPlayerEvent, long elapsedTime, String message) {

    }


    @Override
    public void onMetaData(io.agora.mediaplayer.Constants.MediaPlayerMetadataType mediaPlayerMetadataType, byte[] bytes) {

    }

    @Override
    public void onPlayBufferUpdated(long l) {

    }

    @Override
    public void onPreloadEvent(String src, io.agora.mediaplayer.Constants.MediaPlayerPreloadEvent event) {

    }

    @Override
    public void onCompleted() {

    }

    @Override
    public void onAgoraCDNTokenWillExpire() {

    }

    @Override
    public void onPlayerSrcInfoChanged(SrcInfo from, SrcInfo to) {

    }

    @Override
    public void onPlayerInfoUpdated(PlayerUpdatedInfo info) {

    }

    @Override
    public void onAudioVolumeIndication(int volume) {

    }


    @Override
    public void enablePublishAuth(boolean enable) {
        this.openPublishAuth = enable;
    }

    @Override
    public String getStreamIDForUser(String userId) {
        return "";
    }

    @Override
    public void setInitCallback(IEngineInitCallback initCallback) {
        this.initCallback = initCallback;
    }

    @Override
    public void pushExternalVideoFrame(VideoFrame videoFrame) {
        RtcEngineEx rtcEngine = SAaoraInstance.getInstance().rtcEngine();
        if (rtcEngine == null){
            return;
        }
        ChannelMediaOptions options = SAaoraInstance.getInstance().getOptions();
        if (options == null) return;
        if (!setSource) {
            setSource = true;
            rtcEngine.setExternalVideoSource(true, true, Constants.ExternalVideoSourceType.VIDEO_FRAME);
        }
        options.autoSubscribeVideo = true;
        options.publishCustomVideoTrack = true;
        options.publishEncodedVideoTrack = true;
        SAaoraInstance.getInstance().updateOption(options);
//        SAaoraInstance.getInstance().rtcEngine().setExternalVideoSource(true, true, true);
//        SAaoraInstance.getInstance().rtcEngine().enableVideo();
//        SAaoraInstance.getInstance().rtcEngine().setVideoEncoderConfiguration(new VideoEncoderConfiguration(
//                VD_640x480,
//                FRAME_RATE_FPS_15,
//                STANDARD_BITRATE,
//                ORIENTATION_MODE_ADAPTIVE));
        rtcEngine.pushExternalVideoFrame(videoFrame);
    }

    @Override
    public void pushExternalVideoFrameV2(EGLContext eglContext, final int cameraTextureId, final int cameraWidth, final int cameraHeight, final boolean withAlpha) {
        RtcEngineEx rtcEngine = SAaoraInstance.getInstance().rtcEngine();
        if (rtcEngine == null) return;
        SLogKt.SLogApi.d("RoomChatEngineAgora", "pushExternalVideoFrameV2 first");
        if (!isLogin()) return;
        SLogKt.SLogApi.d("RoomChatEngineAgora", "pushExternalVideoFrameV2 setSource = " + setSource);
        if (!setSource) {
            setSource = true;
            ChannelMediaOptions options = SAaoraInstance.getInstance().getOptions();
            if (options == null) return;
            SLogKt.SLogApi.d("RoomChatEngineAgora", "pushExternalVideoFrameV2 options ok");
            options.publishCustomVideoTrack = true;
            SAaoraInstance.getInstance().updateOption(options);
            rtcEngine.setExternalVideoSource(true, true, Constants.ExternalVideoSourceType.VIDEO_FRAME);
//            VideoEncoderConfiguration.VideoDimensions vd  =  new VideoEncoderConfiguration.VideoDimensions();
            SimulcastStreamConfig ssc = new SimulcastStreamConfig();
            if (SoulRTCSimulcastStreamModel != null) {
                if (SoulRTCSimulcastStreamModel.framerate != 0)
                    ssc.framerate = SoulRTCSimulcastStreamModel.framerate;
                if (SoulRTCSimulcastStreamModel.bitrate != 0)
                    ssc.bitrate = SoulRTCSimulcastStreamModel.bitrate;
                if (SoulRTCSimulcastStreamModel.dimensions != null)
                    ssc.dimensions = SoulRTCSimulcastStreamModel.dimensions;
            }
            rtcEngine.enableDualStreamMode(Constants.VideoSourceType.VIDEO_SOURCE_CUSTOM, enableDualStreamMode, ssc);
        }
        if (eglContext.getNativeHandle() != mEglHandler) {
            if (textureBufferHelper != null) {
                textureBufferHelper.dispose();
                textureBufferHelper = null;
            }
            mEglHandler = eglContext.getNativeHandle();
        }
        if (textureBufferHelper == null) {
            textureBufferHelper = TextureBufferHelper.create("STProcess", new EglBase14.Context(eglContext));
        }
        VideoFrame.Buffer buffer =
                textureBufferHelper.invoke(new Callable<VideoFrame.Buffer>() {
                    @Override
                    public VideoFrame.Buffer call() throws Exception {
                        VideoFrame.Buffer rgbaBuffer = textureBufferHelper.wrapTextureBuffer(cameraWidth, cameraHeight, VideoFrame.TextureBuffer.Type.RGB, cameraTextureId, RendererCommon.convertMatrixToAndroidGraphicsMatrix(matrix));
                        if (withAlpha) {
                            alphaBuffer = textureBufferHelper.parseAlphaData((TextureBuffer) rgbaBuffer, 180, true, false); //使用textureBufferHelper解析出alpha数据
                        }
                        return rgbaBuffer;
                    }
                });
        VideoFrame frame = new VideoFrame(buffer, 0, System.nanoTime());
        if (withAlpha) {
            ByteBuffer alphaBufferTemp = alphaBuffer;
            if (alphaBufferTemp != null) {
                frame.fillAlphaData(alphaBufferTemp);
            }
        }
        rtcEngine.pushExternalVideoFrame(frame);
        SLogKt.SLogApi.d("RoomChatEngineAgora", "pushExternalVideoFrameV2 pushExternalVideoFrame ok");
    }

    @Override
    public void setExternalVideoSourceType(SLExternalVideoSourceType sourceType) {

    }

    @Override
    public boolean isLogin() {
        if (SAaoraInstance.getInstance().rtcEngine() == null) {
            return false;
        }
        SLogKt.SLogApi.d("RoomChatEngineAgora", "isLogin isLogin =" + isLogin);
        return isLogin;
    }

    private void initMediaPlayer() {
        RtcEngineEx rtcEngine = SAaoraInstance.getInstance().rtcEngine();
        if (rtcEngine == null){
            return;
        }
        if (mediaPlayer == null) {
            steamId = rtcEngine.createDataStream(false, false);
            if (syncSteamId == 0) {
                DataStreamConfig config = new DataStreamConfig();
                config.syncWithAudio = true;
                syncSteamId = rtcEngine.createDataStream(config);
            }
            mediaPlayer = rtcEngine.createMediaPlayer();
            mediaPlayer.registerPlayerObserver(this);
            mediaPlayer.setLoopCount(0);
        }
    }

    private void initAudioMediaPlayer() {
        RtcEngine rtcEngine = SAaoraInstance.getInstance().rtcEngine();
        if (audioMediaPlayer == null && rtcEngine != null) {
            DataStreamConfig config = new DataStreamConfig();
            config.syncWithAudio = true;
            steamId = rtcEngine.createDataStream(config);
            syncSteamId = SAaoraInstance.getInstance().rtcEngine().createDataStream(config);
            audioMediaPlayer = rtcEngine.createMediaPlayer();
            audioMediaPlayer.registerPlayerObserver(this);
            audioMediaPlayer.setPlayerOption("play_pos_change_callback", 100);
            audioMediaPlayer.setLoopCount(0);

            if (haveAdjustVolum){
                if (playoutVolume >= 0 && publishSignalVolume >= 0) {
                    setVideoVolume(playoutVolume, publishSignalVolume);
                }
            }
        }
    }

    @Override
    public void sendMessage(byte[] msg) {
        try {
            RtcEngineEx rtcEngine = SAaoraInstance.getInstance().rtcEngine();
            if (rtcEngine == null){
                return;
            }
            if (outSteamId == -1) {
                DataStreamConfig config = new DataStreamConfig();
                config.syncWithAudio = true;
                outSteamId = rtcEngine.createDataStream(config);
            }
            rtcEngine.sendStreamMessage(outSteamId, msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void enableAudioVolumeIndication(int interval, int smooth) {
        RtcEngineEx rtcEngine = SAaoraInstance.getInstance().rtcEngine();
        if (rtcEngine != null) {
            rtcEngine.enableAudioVolumeIndication(interval, smooth, false);
        }
    }

    @Override
    public void setChatType(String type) {
        chatType = type;
    }

    private boolean enableDualStreamMode;
    private cn.soulapp.android.lib.media.zego.SoulRTCSimulcastStreamModel SoulRTCSimulcastStreamModel;

    @Override
    public void enableDualStreamMode(boolean enable, SoulRTCSimulcastStreamModel streamModel) {
        try {
            this.enableDualStreamMode = enable;
            this.SoulRTCSimulcastStreamModel = streamModel;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setRemoteVideoStreamType(int uid, int streamType) {
        try {
            RtcEngineEx rtcEngine = SAaoraInstance.getInstance().rtcEngine();
            if (rtcEngine == null){
                return;
            }
            rtcEngine.setRemoteVideoStreamType(uid, streamType);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setExternalVideoSource() {
        try {
            RtcEngineEx rtcEngine = SAaoraInstance.getInstance().rtcEngine();
            if (rtcEngine == null){
                return;
            }
            rtcEngine.setExternalVideoSource(true, true, Constants.ExternalVideoSourceType.VIDEO_FRAME);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int audioTrack = -1;

    @Override
    public void selectAudioTrack(int audioTrack) {
        if (audioMediaPlayer != null) {
            audioMediaPlayer.selectAudioTrack(audioTrack);
        }
        this.audioTrack = audioTrack;
    }

    @Override
    public long getAudioDuration() {
        if (audioMediaPlayer != null) {
            audioMediaPlayer.getDuration();
        }
        return 0;
    }

    @Override
    public long getAudioCurrentPosition() {
//        if (audioMediaPlayer != null) {
//            audioMediaPlayer.getPlayPosition();
//        }
        return currentAudioPosition;
    }

    public void audioSeekTo(long l) {
        if (audioMediaPlayer != null) {
            audioMediaPlayer.seek(l);
        }
    }

    public SLMediaPlayerState getAudioPlayerState() {
        io.agora.mediaplayer.Constants.MediaPlayerState mediaPlayerState;
        if (audioMediaPlayer != null) {
            mediaPlayerState = audioMediaPlayer.getState();
            if (mediaPlayerState != null) {
                if (mediaPlayerState.equals(PLAYER_STATE_OPEN_COMPLETED)) {
                    return SLMediaPlayerState.PLAYER_STATE_START;
                } else if (mediaPlayerState.equals(PLAYER_STATE_PLAYBACK_COMPLETED) || mediaPlayerState.equals(PLAYER_STATE_PLAYBACK_ALL_LOOPS_COMPLETED)) {
                    return SLMediaPlayerState.PLAYER_STATE_COMPLETE;
                } else if (mediaPlayerState.equals(PLAYER_STATE_FAILED)) {
                    return SLMediaPlayerState.PLAYER_STATE_FAILED;
                } else if (mediaPlayerState.equals(PLAYER_STATE_IDLE)) {
                    return SLMediaPlayerState.PLAYER_STATE_STOP;
                } else if (mediaPlayerState.equals(PLAYER_STATE_PLAYING)) {
                    return SLMediaPlayerState.PLAYER_STATE_PLAYING;
                } else if (mediaPlayerState.equals(PLAYER_STATE_PAUSED)) {
                    return SLMediaPlayerState.PLAYER_STATE_PAUSED;
                }
            }
        }
        return SLMediaPlayerState.PLAYER_STATE_STOP;
    }

    @Override
    public void enableEarAudioEffect(boolean enable) {
        this.enableEarAudioEffect = enable;
    }

    @Override
    public void stopPushExternalVideoFrame() {
        setSource = false;
        ChannelMediaOptions options = SAaoraInstance.getInstance().getOptions();
        if (options == null) return;
        options.publishCustomVideoTrack = false;
        SAaoraInstance.getInstance().updateOption(options);
    }

    @Override
    public void subscribeRemoteStream(boolean isSubscribe) {
        ChannelMediaOptions options = SAaoraInstance.getInstance().getOptions();
        if (options == null) return;
        options.autoSubscribeAudio = isSubscribe;
        options.autoSubscribeVideo = isSubscribe;
        SAaoraInstance.getInstance().updateOption(options);

        Map<String, Object> map = new HashMap<>();
        map.put("state", isSubscribe ? "1" : "0");
        track("RTC_Auto_Subscribe", map);
    }

    @Override
    public void setAudioPlayerCallBack(IAudioPlayerCallBack callBack) {
        this.audioPlayerCallBack = callBack;
    }

    @Override
    public void uploadLog(Application context, String soPath, String userId, String userName) {

    }

    @Override
    public void enableKtv(boolean enable) {
//        RtcEngineEx rtcEngine = SAaoraInstance.getInstance().rtcEngine();
//        if (rtcEngine == null) {
//            return;
//        }
//        if (enable) {
//            rtcEngine.setAudioScenario(Constants.AUDIO_SCENARIO_GAME_STREAMING);
//        } else {
//            rtcEngine.setAudioScenario(Constants.AUDIO_SCENARIO_DEFAULT);
//        }
    }

    private void setDefaultAudioRoutetoSpeakerphoneWapper(boolean isEnable){
        RtcEngineEx rtcEngineEx = SAaoraInstance.getInstance().rtcEngine();
        if (rtcEngineEx == null){
            SLogKt.SLogApi.e(TAG, "setDefaultAudioRoutetoSpeakerphoneWapper rtcEngineEx==null, isEnable="+isEnable);
            return;
        }
        SLogKt.SLogApi.d("RoomChatEngineAgora", "setDefaultAudioRoutetoSpeakerphoneWapper isLogin ="+isLogin + ", isEnable = "+isEnable);
        if(isLogin){
            rtcEngineEx.setEnableSpeakerphone(isEnable);
        } else {
            rtcEngineEx.setDefaultAudioRoutetoSpeakerphone(isEnable);
        }

        Map<String, Object> map = new HashMap<>();
        map.put("state", isEnable ? "1" : "0");
        track("RTC_Speakerphone", map);
    }

    @Override
    public void setAudioRecordCallBack(IAudioRecordCallBack callback) {
        if (null != iMediaRecorderCallback && null != audioRecordCallBack) {
            return;
        }
        initAgoraMediaRecorder();
        initRtcConnection();
        if (null == rtcConnection || null == agoraMediaRecorder) {
            return;
        }
        this.audioRecordCallBack = callback;
        agoraMediaRecorder.setMediaRecorderObserver(rtcConnection, iMediaRecorderCallback = new IMediaRecorderCallback() {

            @Override
            public void onRecorderStateChanged(final int state, final int error) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        IAudioRecordCallBack audioRecordCallBack = AgoraChatRoomEngine.this.audioRecordCallBack;
                        if (state == AgoraMediaRecorder.RECORDER_STATE_ERROR || state == AgoraMediaRecorder.RECORDER_STATE_STOP) {
                            if (null != audioRecordingParams && null != audioRecordCallBack) {
                                audioRecordCallBack.onAudioRecordStateUpdate(audioRecordingParams.filePath);
                            }
                        }
                    }
                });

            }

            @Override
            public void onRecorderInfoUpdated(final RecorderInfo info) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        IAudioRecordCallBack audioRecordCallBack = AgoraChatRoomEngine.this.audioRecordCallBack;
                        if (null != info && null != audioRecordCallBack) {
                            audioRecordCallBack.onRecordStateUpdate(info.fileName, info.durationMs, info.fileSize);
                        }
                    }
                });
            }
        });
    }

    private void initRtcConnection(){
        if(null == rtcConnection){
            rtcConnection = new RtcConnection();
            rtcConnection.channelId = getRoomId();
            rtcConnection.localUid = Integer.parseInt(userId);
        }
    }

    private void initAgoraMediaRecorder(){
        RtcEngine rtcEngine = SAaoraInstance.getInstance().rtcEngine();
        if (null == rtcEngine) {
            return;
        }
        if (null == agoraMediaRecorder) {
            agoraMediaRecorder = AgoraMediaRecorder.getMediaRecorder(rtcEngine);
        }
    }

    @Override
    public void removeAudioRecordCallBack() {
        this.audioRecordCallBack = null;
        if (null != agoraMediaRecorder && null != iMediaRecorderCallback && null != rtcConnection) {
            agoraMediaRecorder.setMediaRecorderObserver(rtcConnection, null);
        }
        if (null != agoraMediaRecorder) {
            agoraMediaRecorder.release();
            agoraMediaRecorder = null;
        }
    }

    @Override
    public void startAudioRecord(AudioRecordingParams params) {
        initAgoraMediaRecorder();
        initRtcConnection();
        if (null == rtcConnection || null == agoraMediaRecorder) {
            return;
        }
        this.audioRecordingParams = params;
        AgoraMediaRecorder.MediaRecorderConfiguration config = new AgoraMediaRecorder
                .MediaRecorderConfiguration(params.filePath, AgoraMediaRecorder.CONTAINER_AAC,
                AgoraMediaRecorder.STREAM_TYPE_AUDIO, 6000000, 1000);
        agoraMediaRecorder.startRecording(rtcConnection, config);
    }

    @Override
    public void stopAudioRecord() {
        initAgoraMediaRecorder();
        initRtcConnection();
        if (null == rtcConnection || null == agoraMediaRecorder) {
            return;
        }
        agoraMediaRecorder.stopRecording(rtcConnection);
    }

    @Override
    public String getStreamId() {
        return null;
    }

    @Override
    public void isMonitorHit(boolean isHit) {
        this.isHit = isHit;
    }

    @Override
    public void startGame(GameParams gameParams) {

    }

    @Override
    public void stopGame() {

    }

    @Override
    public String getGameDeviceId() {
        return null;
    }

    @Override
    public void startPlayPublicStream(String publicStreamId) {

    }

    @Override
    public void stopPlayPublicStream(String publicStreamId) {

    }

    @Override
    public void subscribeStream(boolean isSubscribe, String uid, int type) {

    }

    @Override
    public void setLeaderSingerUid(String uidEcpt) {

    }

    @Override
    public int getPlaybackVolume() {
        return playbackVolume;
    }

    @Override
    public int getRecordingSignalVolume() {
        return recordingSignalVolume;
    }

    private void track(String eventId, Map<String, Object> map){
        track(null, eventId, map);
    }

    private void track(String roomId, String eventId, Map<String, Object> map){
        if(map == null){
            map = new HashMap<>();
        }
        map.put("channelId", TextUtils.isEmpty(roomId) ? getRoomId() : roomId);
        map.put("channelType","1");
        if(!TextUtils.isEmpty(businessType)){
            map.put("businessType", businessType);
        }
        SoulAnalyticsV2.getInstance().onEvent(cn.soulapp.android.lib.analyticsV2.Const.EventType.INDICATORS, eventId, map);
    }

}