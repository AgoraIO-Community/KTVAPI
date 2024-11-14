package cn.soulapp.android.lib.media.agroa.interfaces;

import java.nio.ByteBuffer;

import io.agora.mediaplayer.IMediaPlayer;
import io.agora.mediaplayer.IMediaPlayerCustomDataProvider;

public interface IAgoraRtcBridge {
    void setEventHandler(IAgoraRtcBridgeEventHandler handler);

    // ================== 实时合唱 ==================

    IMediaPlayer getMediaPlayer();

    void leaveKtvRoom();

    void playKTVEncryptAudio(
            String url, // TODO Change from PlayKTVParams
            int role,   // TODO Change from PlayKTVParams
            String chorusToken, // TODO Change from PlayKTVParams
            String chorusChannelId, // TODO Change from PlayKTVParams
            String curSingerUid,    // TODO Change from PlayKTVParams
            int playbackSignalVolume,  // TODO Change from PlayKTVParams
            String audioUniId, // TODO Change from PlayKTVParams
            Boolean isAccompanyDelayPositionChange,
            IMediaPlayerCustomDataProvider customDataProvider // TODO null if not encryption
    );

    void stopAudio();

    void setNewLeadSinger(
            int uid,
            String url, // TODO Change from PlayKTVParams
            String chorusToken, // TODO Change from PlayKTVParams
            String chorusChannelId, // TODO Change from PlayKTVParams
            String curSingerUid,    // TODO Change from PlayKTVParams
            int playbackSignalVolume,  // TODO Change from PlayKTVParams,
            String audioUniId, // TODO Change from PlayKTVParams
            Boolean isAccompanyDelayPositionChange,
            IMediaPlayerCustomDataProvider customDataProvider);

    void pauseAudio();

    void resumeAudio();

    void selectAudioTrack(int audioTrack);

    boolean isAudioPlaying();

    long getAudioDuration();

    long getAudioCurrentPosition();

    void audioSeekTo(long l);

    io.agora.mediaplayer.Constants.MediaPlayerState getAudioPlayerState();

    void setAudioDelay(int audioDelay);

    void onRecordAudioFrame(ByteBuffer buffer, long renderTimeMs);

    void enableMic(boolean enable);

    String getChorusToken();

    void setVideoVolume(int volume);

    void setVideoVolume(int playoutVolume, int publishSignalVolume);


    // ================ 声音卡片 ================

    void adjustAudioMixingVolume(int volume);

    void setEffectVolume(int soundID, int volume);

    void setPlaybackSignalVolume(int playbackSignalVolume);

    int getPlaybackVolume();

    // 开始录制声音卡片
    void startAudioCardRecording(String filePath);

    // 结束录制声音卡片
    void stopAudioCardRecording();

    // 播放录制
    void startAudioCardPlayback(String filePath);

    // 暂停播放
    void pauseAudioCardPlayback();

    // 恢复播放
    void resumeAudioCardPlayback();

    // 停止播放
    void stopAudioCardPlayback();
}
