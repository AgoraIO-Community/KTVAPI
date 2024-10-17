package cn.soulapp.android.lib.media.zego;

import static io.agora.mediaplayer.Constants.MediaPlayerState.PLAYER_STATE_FAILED;
import static io.agora.mediaplayer.Constants.MediaPlayerState.PLAYER_STATE_IDLE;
import static io.agora.mediaplayer.Constants.MediaPlayerState.PLAYER_STATE_OPEN_COMPLETED;
import static io.agora.mediaplayer.Constants.MediaPlayerState.PLAYER_STATE_PAUSED;
import static io.agora.mediaplayer.Constants.MediaPlayerState.PLAYER_STATE_PLAYBACK_ALL_LOOPS_COMPLETED;
import static io.agora.mediaplayer.Constants.MediaPlayerState.PLAYER_STATE_PLAYBACK_COMPLETED;
import static io.agora.mediaplayer.Constants.MediaPlayerState.PLAYER_STATE_PLAYING;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

// TODO: Add imports
//import cn.soul.insight.log.core.SLogKt;
//import cn.soulapp.android.lib.media.Const;
//import cn.soulapp.android.lib.media.agroa.AgroaEngineConfig;
//import cn.soulapp.android.lib.media.agroa.AgroaEngineEventHandler;
//import cn.soulapp.android.lib.media.agroa.SAaoraInstance;
//import cn.soulapp.android.lib.media.zego.utils.GsonUtils;
//import cn.soulapp.android.lib.media.IAudioPlayerCallBack;
//import cn.soulapp.android.lib.media.zego.interfaces.IRoomCallback;
import cn.soulapp.android.lib.media.Const;
import cn.soulapp.android.lib.media.IAudioPlayerCallBack;
import cn.soulapp.android.lib.media.SLMediaPlayerState;
import cn.soulapp.android.lib.media.agroa.AgroaEngineConfig;
import cn.soulapp.android.lib.media.agroa.AgroaEngineEventHandler;
import cn.soulapp.android.lib.media.agroa.SAaoraInstance;
import cn.soulapp.android.lib.media.zego.beans.PlayKTVParams;
import cn.soulapp.android.lib.media.zego.beans.StreamMessage;
import cn.soulapp.android.lib.media.zego.interfaces.IAgoraKTVChorusHelper;
import cn.soulapp.android.lib.media.zego.interfaces.IAgoraKTVSyncProcess;
import cn.soulapp.android.lib.media.zego.interfaces.IRoomCallback;
import cn.soulapp.android.lib.media.zego.utils.GsonUtils;
import io.agora.mediaplayer.IMediaPlayer;
import io.agora.mediaplayer.IMediaPlayerObserver;
import io.agora.mediaplayer.data.PlayerUpdatedInfo;
import io.agora.mediaplayer.data.SrcInfo;
import io.agora.rtc2.ChannelMediaOptions;
import io.agora.rtc2.Constants;
import io.agora.rtc2.DataStreamConfig;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcConnection;
import io.agora.rtc2.RtcEngineEx;

/**
 * 声网KTV合唱帮助类
 * Created by 罗康辉 on 2022/11/15
 */
class AgoraKTVChorusHelper implements IAgoraKTVChorusHelper, IMediaPlayerObserver {
    private static final String TAG = "AgoraKTVChorusHelper";

    private AgoraKTVConfig mKtvConfig;
    private int mRole;
    private final RtcEngineEx mRtcEngine;
    private IMediaPlayer mMediaPlayer;
    private AgroaEngineConfig mConfig;
    private String mChorusToken; //主唱人声流token
    private volatile boolean mHasJoinChannelEx;
    private RtcConnection mRtcConnectionEx;
    private boolean mHasOnStart;
    private int mLeaderUid;
    private boolean mEnableMic = true;
    private volatile boolean mHasPlay;

    // TODO why different sid
    private int steamId;
    private int syncSteamId;

    private MediaPlayerCustomDataProvider customDataProvider;

    private IAgoraKTVSyncProcess syncProcessHelper;//KTV合唱同步

    private boolean isAccompanyDelayPositionChange = false;

    private String audioUniId;

    private boolean isDelayLocalSendPosition = true;

    private long audioDuration = 0;//

    // TODO Add variables
    private int audioDelay;

    private long currentAudioPosition;


    // TODO Add variables
    private IAudioPlayerCallBack audioPlayerCallBack;

    private IRoomCallback iRoomCallback;

    private String audioUrl;

    private int audioTrack = -1;

    private boolean haveAdjustVolum;

    private Handler mHandler = new Handler(Looper.getMainLooper());

    public AgoraKTVChorusHelper(RtcEngineEx rtcEngine) {
        this.mRtcEngine = rtcEngine;
//        this.audioPlayerCallBack = audioPlayerCallBack;
//        this.iRoomCallback = iRoomCallback;
        initAudioMediaPlayer();
    }

    @Override
    public void setAudioPlayerCallBack(IAudioPlayerCallBack audioPlayerCallBack) {
        this.audioPlayerCallBack = audioPlayerCallBack;
    }

    @Override
    public void setRoomCallBack(IRoomCallback iRoomCallback) {
        this.iRoomCallback = iRoomCallback;
    }

    @Override
    public void release() {
        IAgoraKTVSyncProcess syncProcessHelper = this.syncProcessHelper;
        if (syncProcessHelper != null) {
            syncProcessHelper.stop();
            this.syncProcessHelper = null;
        }
        if (mMediaPlayer != null) {
            mMediaPlayer.unRegisterPlayerObserver(this);
            mMediaPlayer.destroy();
            mMediaPlayer = null;
        }
        audioDuration = 0;
        haveAdjustVolum = false;
    }

    @Override
    public void switchSingerRole(int role) {
        if (mRole == Const.KTV_ROLE_AUDIENCE && role == Const.KTV_ROLE_LEADER_SINGER) {
            startPlay();
        } else if (mRole == Const.KTV_ROLE_AUDIENCE && role == Const.KTV_ROLE_ACCOMPANY_SINGER) {
            startPlay();
        } else if (mRole == Const.KTV_ROLE_LEADER_SINGER && role == Const.KTV_ROLE_AUDIENCE) {
            stopPlay();
        } else if (mRole == Const.KTV_ROLE_ACCOMPANY_SINGER && role == Const.KTV_ROLE_AUDIENCE) {
            stopPlay();
        } else if (mRole == Const.KTV_ROLE_ACCOMPANY_SINGER && role == Const.KTV_ROLE_LEADER_SINGER) {
            if (syncProcessHelper != null) {
                syncProcessHelper.stop();
            }
            //把背景音乐发布到主频道
            ChannelMediaOptions options = new ChannelMediaOptions();
            options.publishMediaPlayerAudioTrack = true;
            options.publishMediaPlayerId = mMediaPlayer.getMediaPlayerId();
            options.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER;
            mRtcEngine.updateChannelMediaOptions(options);

            //加入频道2（人声）
            options = new ChannelMediaOptions();
            options.autoSubscribeAudio = false;
            options.autoSubscribeVideo = false;
            options.publishMicrophoneTrack = false;
            options.enableAudioRecordingOrPlayout = false;
            options.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER;
            options.publishDirectCustomAudioTrack = true;
            mRtcEngine.updateChannelMediaOptionsEx(options, mRtcConnectionEx);
            mRtcEngine.muteRemoteAudioStreamEx(mLeaderUid, false, mRtcConnectionEx);

            mRtcEngine.setParameters("{\"che.audio.custom_bitrate\":80000}");
        }
        mRole = role;
    }

    @Override
    public void playKTVEncryptAudio(PlayKTVParams params, Boolean isAccompanyDelayPositionChange) {
        String url = params.url;
        int role = params.role;
        this.isAccompanyDelayPositionChange = isAccompanyDelayPositionChange;
        this.audioUniId = params.audioUniId;
        isDelayLocalSendPosition = true;
        mRole = role;

        if (!haveAdjustVolum) {
            setLocalVolume(100);
            if (role == Const.KTV_ROLE_ACCOMPANY_SINGER) {
                //伴唱
                mMediaPlayer.adjustPlayoutVolume(25);
            } else {
                setVideoVolume(50);
            }
        }

        audioDuration = 0;
        boolean changeSource =
                !url.equals(audioUrl);
        if (mMediaPlayer.getState() == io.agora.mediaplayer.Constants.MediaPlayerState.PLAYER_STATE_PLAYING && changeSource) {
            mMediaPlayer.stop();
        }
        audioUrl = url;
        // TODO params.playType 是不是一定是 playTypeEncryption
        if (params.playType == Const.KtvAudioPlayType.playTypeEncryption) {
            customDataProvider = new MediaPlayerCustomDataProvider();
            customDataProvider.setUrl(url);
            customDataProvider.setMediaPlayerFileReader(params.fileReader);
        }

        RtcEngineEx rtcEngine = SAaoraInstance.getInstance().rtcEngine();
        if (rtcEngine == null) {
            Log.e(TAG, "playKTVEncryptAudio rtcEngine==null");
            return;
        }
        AgroaEngineConfig config = SAaoraInstance.getInstance().getConfig();

        AgoraKTVConfig ktvConfig = new AgoraKTVConfig();
        ktvConfig.rtcEngine = mRtcEngine;
        ktvConfig.mediaPlayer = mMediaPlayer;
        ktvConfig.role = role;
        ktvConfig.agroaEngineConfig = config;
        ktvConfig.chorusToken = params.chorusToken;
        ktvConfig.chorusChannelId = params.chorusChannelId;
        ktvConfig.leaderUid = Integer.parseInt(params.curSingerUid);
        ktvConfig.syncSteamId = syncSteamId;
        ktvConfig.playbackSignalVolume = params.playbackSignalVolume;
        this.mKtvConfig = ktvConfig;
        this.mRole = ktvConfig.role;
        this.mConfig = ktvConfig.agroaEngineConfig;
        this.mChorusToken = ktvConfig.chorusToken;
        this.mLeaderUid = ktvConfig.leaderUid;
        Log.i(TAG, "new AgoraKTVChorusHelper :mChorusToken="+ mChorusToken
                + " ,chorusChannelId="+ ktvConfig.chorusChannelId
                + " ,mLeaderUid="+mLeaderUid + " ,mRole="+mRole);

        stopPlay();
        enableMic(mEnableMic);
        startPlay();

        if (syncProcessHelper != null) {
            syncProcessHelper.stop();
        }
        syncProcessHelper = new AgoraKTVSyncProcessNTPHelper(ktvConfig);
        if (role == Const.KTV_ROLE_ACCOMPANY_SINGER) {
            syncProcessHelper.startSyncProcess();
        }

        if (mMediaPlayer.getState() != io.agora.mediaplayer.Constants.MediaPlayerState.PLAYER_STATE_PLAYING || changeSource) {
            if (mMediaPlayer.getState() != io.agora.mediaplayer.Constants.MediaPlayerState.PLAYER_STATE_PAUSED) {
                if (params.playType == Const.KtvAudioPlayType.playTypeEncryption) {
                    mMediaPlayer.openWithCustomSource(0, customDataProvider);
                } else {
                    mMediaPlayer.open(url, 0);
                }
            }
        }
    }

    @Override
    public void stopAudio() {
        mRole = 0;
        audioUniId = "";
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            audioDuration = 0;
            ChannelMediaOptions options = SAaoraInstance.getInstance().getOptions();
            if (options == null) return;
            options.publishMediaPlayerAudioTrack = false;
            options.publishMediaPlayerVideoTrack = false;
            SAaoraInstance.getInstance().updateOption(options);
        }

        if (syncProcessHelper != null) {
            syncProcessHelper.stop();
            syncProcessHelper = null;
        }
        stopPlay();
    }

    /**
     * 设置新的主唱
     * @param uid 新主唱uid
     */
    @Override
    public void setNewLeadSinger(int uid, PlayKTVParams params, Boolean isAccompanyDelayPositionChange) {
        if (mRole == Const.KTV_ROLE_LEADER_SINGER && mConfig.uid != uid) {
            mLeaderUid = uid;
        } else if (mRole == Const.KTV_ROLE_AUDIENCE && mConfig.uid == uid) {
            switchSingerRole(Const.KTV_ROLE_ACCOMPANY_SINGER);
            playKTVEncryptAudio(params, isAccompanyDelayPositionChange);
            mHandler.postDelayed(() -> {
                switchSingerRole(Const.KTV_ROLE_LEADER_SINGER);
                mLeaderUid = uid;
            }, 3500);
        }
    }

    @Override
    public void pauseAudio() {
        if (mMediaPlayer != null) {
            mMediaPlayer.pause();
        }
    }

    @Override
    public void resumeAudio() {
        if (mMediaPlayer != null) {
            mMediaPlayer.resume();
        }
    }

    @Override
    public void selectAudioTrack(int audioTrack) {
        if (mMediaPlayer != null) {
            mMediaPlayer.selectAudioTrack(audioTrack);
        }
        this.audioTrack = audioTrack;
    }

    @Override
    public boolean isAudioPlaying() {
        return mMediaPlayer != null && mMediaPlayer.getState() == io.agora.mediaplayer.Constants.MediaPlayerState.PLAYER_STATE_PLAYING;
    }

    @Override
    public long getAudioDuration() {
        if (mMediaPlayer != null) {
            mMediaPlayer.getDuration();
        }
        return 0;
    }

    @Override
    public long getAudioCurrentPosition() {
        return currentAudioPosition;
    }

    @Override
    public void audioSeekTo(long l) {
        if (mMediaPlayer != null) {
            mMediaPlayer.seek(l);
        }
    }

    @Override
    public SLMediaPlayerState getAudioPlayerState() {
        io.agora.mediaplayer.Constants.MediaPlayerState mediaPlayerState;
        if (mMediaPlayer != null) {
            mediaPlayerState = mMediaPlayer.getState();
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
    public void setAudioDelay(int audioDelay) {
        this.audioDelay = audioDelay;
        if (syncProcessHelper != null) {
            syncProcessHelper.setAudioDelay(audioDelay);
        }
        Log.d("RoomChatEngineAgora", "audioPlayoutDelay="+audioDelay);
    }

    /**
     * 主唱or伴唱开始播放
     */
    private void startPlay() {
        if (mHasPlay){
            return;
        }
        mHasPlay = true;
        if (mRole == Const.KTV_ROLE_LEADER_SINGER){
            leaderPlay();
            mRtcEngine.setParameters("{\"che.audio.custom_bitrate\":80000}");
        }else if (mRole == Const.KTV_ROLE_ACCOMPANY_SINGER){
            chorusPlay();
            mRtcEngine.setParameters("{\"che.audio.custom_bitrate\":48000}");
        }

        mRtcEngine.setParameters("{\"rtc.video.enable_sync_render_ntp_broadcast_dynamic\":false}");//主播关闭多端同步，动态设置
    }

    /**
     * 主唱or伴唱停止播放
     */
    private void stopPlay() {
        if (!mHasPlay){
            return;
        }
        mHasPlay = false;
        if (mRole == Const.KTV_ROLE_LEADER_SINGER){
            leaderLeave();
        }else if (mRole == Const.KTV_ROLE_ACCOMPANY_SINGER){
            chorusLeave();
        }
        mRtcEngine.adjustPlaybackSignalVolume(100);//恢复收流音量
        mRtcEngine.setParameters("{\"rtc.video.enable_sync_render_ntp_broadcast_dynamic\":true}");//主播启用多端同步，动态设置
        mRtcEngine.setParameters("{\"che.audio.custom_bitrate\":48000}");//
    }

    /**
     * 主唱推送人声流
     */
    @Override
    public void onRecordAudioFrame(ByteBuffer buffer, long renderTimeMs) {
        if (mEnableMic && mRole == Const.KTV_ROLE_LEADER_SINGER && mHasJoinChannelEx) {
            //推送人声流
            mRtcEngine.pushDirectAudioFrame(buffer,
                    renderTimeMs, 48000, 1);
        }
    }

    /**
     * 开始播放
     */
    private void onStartPlay() {
        if (mHasOnStart){
            return;
        }
        mHasOnStart = true;
        if (mRole == Const.KTV_ROLE_ACCOMPANY_SINGER){
            //伴唱
            // mute主频道
            mRtcEngine.muteRemoteAudioStream(mLeaderUid, true);
            //取消mute主唱人声
            mRtcEngine.muteRemoteAudioStreamEx(mLeaderUid, false, mRtcConnectionEx);
            mRtcEngine.adjustPlaybackSignalVolume(getPlaybackSignalVolume());
        }

        Log.i(TAG, "onStartPlay");
    }

    /**
     * 麦克风是否禁用
     */
    @Override
    public void enableMic(boolean enable) {
        mEnableMic = enable;
    }


    @Override
    public String getChorusToken() {
        return mChorusToken;
    }

    /***
     * 主唱开始播放
     */
    private void leaderPlay() {
        //把背景音乐发布到主频道
        ChannelMediaOptions options = new ChannelMediaOptions();
        options.publishMediaPlayerAudioTrack = true;
        options.publishMediaPlayerId = mMediaPlayer.getMediaPlayerId();
        options.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER;
        mRtcEngine.updateChannelMediaOptions(options);


        //加入频道2（人声）
        options = new ChannelMediaOptions();
        options.autoSubscribeAudio = false;
        options.autoSubscribeVideo = false;
        options.publishMicrophoneTrack = false;
        options.enableAudioRecordingOrPlayout = false;
        options.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER;
        options.publishDirectCustomAudioTrack = true;
        IRtcEngineEventHandler handler = new IRtcEngineEventHandler() {
            @Override
            public void onJoinChannelSuccess(String channel, int uid, int elapsed)
            {
                Log.i(TAG, "主唱 ex 频道 onJoinChannelSuccess");
                mHasJoinChannelEx = true;
                mRtcEngine.enableAudioVolumeIndicationEx(1500, 3, false, mRtcConnectionEx);
            }
            @Override
            public void onLeaveChannel(RtcStats stats) {
                Log.i(TAG, "主唱 ex 频道 onLeaveChannel");
                mHasJoinChannelEx = false;
            }

            @Override
            public void onAudioVolumeIndication(AudioVolumeInfo[] speakers, int totalVolume) {
                super.onAudioVolumeIndication(speakers, totalVolume);
                AgroaEngineEventHandler eventHandler = SAaoraInstance.getInstance().getEventHandler();
                if (eventHandler != null) {
                    eventHandler.rtcEventHandler.onAudioVolumeIndication(speakers, totalVolume);
                }
            }

            @Override
            public void onUserJoined(int uid, int elapsed) {
                super.onUserJoined(uid, elapsed);
                // 接唱人员加入
                if (uid == mLeaderUid) {
                    mHandler.postDelayed(() -> {
                        switchSingerRole(Const.KTV_ROLE_AUDIENCE);
                    }, 3000);
                }
            }
        };
        leaderLeave();
        mRtcEngine.setAudioScenario(Constants.AUDIO_SCENARIO_CHORUS);
        mRtcConnectionEx = new RtcConnection();
        mRtcConnectionEx.channelId = mKtvConfig.chorusChannelId;
        mRtcConnectionEx.localUid = mConfig.uid;
        mRtcEngine.joinChannelEx(mChorusToken, mRtcConnectionEx,
                options, handler);

        //主唱在有人加入合唱后才需要设置收流音量
//        mRtcEngine.adjustPlaybackSignalVolume(getPlaybackSignalVolume());
        Log.i(TAG, "leaderPlay");
    }

    /**
     * 主唱离开合唱，退出EX频道
     */
    private void leaderLeave() {
        if (mRtcConnectionEx == null) {
            return;
        }
        mHasJoinChannelEx = false;
        ChannelMediaOptions options = new ChannelMediaOptions();
        options.publishDirectCustomAudioTrack = false;
        mRtcEngine.updateChannelMediaOptionsEx(options,
                mRtcConnectionEx);
        mRtcEngine.leaveChannelEx(mRtcConnectionEx);
        mRtcEngine.setAudioScenario(Constants.AUDIO_SCENARIO_GAME_STREAMING);
        mRtcConnectionEx = null;
        Log.i(TAG, "leaderLeave");
    }


    /**
     * 伴唱开始播放
     */
    private void chorusPlay() {
        //加入频道2（主唱人声）
        ChannelMediaOptions options = new ChannelMediaOptions();
        options.autoSubscribeAudio = true;
        options.autoSubscribeVideo = false;
        options.publishMicrophoneTrack = false;
        options.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER;
        IRtcEngineEventHandler handler = new IRtcEngineEventHandler() {
            @Override
            public void onJoinChannelSuccess(String channel, int uid, int
                    elapsed) {
                Log.i(TAG, "伴唱 ex 频道 onJoinChannelSuccess");
            }
            @Override
            public void onLeaveChannel(RtcStats stats) {
                Log.i(TAG, "伴唱 ex 频道 onLeaveChannel");
            }
        };
        chorusLeave();
        mRtcEngine.setAudioScenario(Constants.AUDIO_SCENARIO_CHORUS);
        mRtcConnectionEx = new RtcConnection();
        mRtcConnectionEx.channelId = mKtvConfig.chorusChannelId;
        mRtcConnectionEx.localUid = mConfig.uid;
        mRtcEngine.joinChannelEx(mChorusToken, mRtcConnectionEx,
                options, handler);

        //先mute主唱人声；等播放开始后，mute主频道，放开人声频道
        mRtcEngine.muteRemoteAudioStreamEx(mLeaderUid, true, mRtcConnectionEx);
        Log.i(TAG, "chorusPlay");
    }

    private void chorusLeave() {
        if (mRtcConnectionEx == null){
            return;
        }
        mRtcEngine.leaveChannelEx(mRtcConnectionEx);
        mRtcEngine.setAudioScenario(Constants.AUDIO_SCENARIO_GAME_STREAMING);
        //取消mute主唱bgm+人声频道
        mRtcEngine.muteRemoteAudioStream(mLeaderUid, false);
        mRtcConnectionEx = null;
        Log.i(TAG, "chorusLeave");
    }

    private int getPlaybackSignalVolume(){
        return mKtvConfig.playbackSignalVolume;
    }


    private void initAudioMediaPlayer() {
        if (mMediaPlayer == null && mRtcEngine != null) {
            DataStreamConfig config = new DataStreamConfig();
            config.syncWithAudio = true;
            steamId = mRtcEngine.createDataStream(config);
            syncSteamId = mRtcEngine.createDataStream(config);
            mMediaPlayer = mRtcEngine.createMediaPlayer();
            mMediaPlayer.registerPlayerObserver(this);
            mMediaPlayer.setPlayerOption("play_pos_change_callback", 100);
            mMediaPlayer.setLoopCount(0);

            if (haveAdjustVolum) {
                if (playoutVolume >= 0 && publishSignalVolume >= 0) {
                    setVideoVolume(playoutVolume, publishSignalVolume);
                }
            }
        }
    }

    private void setLocalVolume(int volume) {
        RtcEngineEx rtcEngine = SAaoraInstance.getInstance().rtcEngine();
        if (rtcEngine == null) return;
        haveAdjustVolum = true;
        rtcEngine.adjustRecordingSignalVolume(volume * 2);//本地需要发布的人声
        rtcEngine.setInEarMonitoringVolume(volume);//本地耳返音量
    }

    @Override
    public void setVideoVolume(int volume) {
        if (mMediaPlayer != null) {
            mMediaPlayer.adjustPlayoutVolume(playoutVolume);
            mMediaPlayer.adjustPublishSignalVolume(publishSignalVolume);
        }
    }

    private int playoutVolume;
    private int publishSignalVolume;

    @Override
    public void setVideoVolume(int playoutVolume, int publishSignalVolume) {
        if (mMediaPlayer != null) {
            mMediaPlayer.adjustPlayoutVolume(playoutVolume);
            mMediaPlayer.adjustPublishSignalVolume(publishSignalVolume);
        }
        this.playoutVolume = playoutVolume;
        this.publishSignalVolume = publishSignalVolume;
        haveAdjustVolum = true;
    }

    // ================== IMediaPlayerObserver ==================
    @Override
    public void onPlayerStateChanged(io.agora.mediaplayer.Constants.MediaPlayerState mediaPlayerState, io.agora.mediaplayer.Constants.MediaPlayerError mediaPlayerError) {
        if (audioUrl != null) {
            if (mediaPlayerState.equals(PLAYER_STATE_OPEN_COMPLETED)) {
                audioDuration = mMediaPlayer.getDuration();
                if (mRole != Const.KTV_ROLE_ACCOMPANY_SINGER) {
                    mMediaPlayer.play();
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
            onStartPlay();
        }

        RtcEngineEx rtcEngineEx = SAaoraInstance.getInstance().rtcEngine();

        //多线程NPE避免
        IAgoraKTVSyncProcess syncProcessHelper = this.syncProcessHelper;

        if (rtcEngineEx != null && mRole == Const.KTV_ROLE_LEADER_SINGER && mMediaPlayer != null) {
            StreamMessage streamMessage = new StreamMessage();
            streamMessage.type = StreamMessage.TYPE_PLAY_STATUS;
            if (syncProcessHelper != null) {
                streamMessage.currentTimeStamp = String.valueOf(syncProcessHelper.getNtpTime());
            }
            streamMessage.currentDuration = String.valueOf(mMediaPlayer.getPlayPosition());
            if (audioDuration <= 0) {
                audioDuration = mMediaPlayer.getDuration();
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
        if (!isDelayLocalSendPosition || mRole == Const.KTV_ROLE_LEADER_SINGER
                || (mRole == Const.KTV_ROLE_ACCOMPANY_SINGER
                && (!isAccompanyDelayPositionChange || Math.abs(l - currentAudioPosition) < 1000))) {
            isDelayLocalSendPosition = false;
            currentAudioPosition = l;
            if (null != audioPlayerCallBack) {
                // TODO 这个 uid 不知道是干啥的
                audioPlayerCallBack.onAudioPositionChanged("", currentAudioPosition, audioUniId, false);
                //audioPlayerCallBack.onAudioPositionChanged(uid, currentAudioPosition, audioUniId, false);
            }
            RtcEngineEx rtcEngine = SAaoraInstance.getInstance().rtcEngine();
            if (rtcEngine!=null && mRole == Const.KTV_ROLE_LEADER_SINGER) {
                StreamMessage streamMessage = new StreamMessage();
                streamMessage.type = StreamMessage.TYPE_PLAY_STATUS;
                if (syncProcessHelper != null) {
                    streamMessage.currentTimeStamp = String.valueOf(syncProcessHelper.getNtpTime());
                }
                streamMessage.currentDuration = String.valueOf(l - audioDelay);
                streamMessage.audioDelay = String.valueOf(audioDelay);
                if (audioDuration <= 0) {
                    audioDuration = mMediaPlayer.getDuration();
                    Log.e(TAG, "onPositionChanged getDuration:"+audioDuration);
                }
                streamMessage.audioUniId = audioUniId;
                streamMessage.duration = String.valueOf(audioDuration);
                io.agora.mediaplayer.Constants.MediaPlayerState mediaPlayerState = null;
                if (syncProcessHelper != null){
                    mediaPlayerState = syncProcessHelper.getMediaPlayerState();
                }
                if (mediaPlayerState == null){
                    mediaPlayerState = mMediaPlayer.getState();
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

    // ================== onStreamMessage ==================
    @Override
    public boolean onStreamMessage(int uid, int streamId, StreamMessage msg) {
        //多线程NPE避免
        IAgoraKTVSyncProcess syncProcessHelper = this.syncProcessHelper;
        if (syncProcessHelper != null) {
            return syncProcessHelper.onStreamMessage(uid, streamId, msg);
        }
        return false;
    }
}
