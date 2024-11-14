package cn.soulapp.android.lib.media.agroa.interfaces;

import io.agora.rtc2.IRtcEngineEventHandler;

public interface IAgoraRtcBridgeEventHandler {
    // 实时合唱 mpk 进度回调
    void onPositionChanged(long position, String audioUniId);

    // 实时合唱 mpk 播放状态回调
    void onPlayerStateChanged(io.agora.mediaplayer.Constants.MediaPlayerState mediaPlayerState, io.agora.mediaplayer.Constants.MediaPlayerError mediaPlayerError);

    // 合唱子频道音频数据回调
    void onAudioVolumeIndication(IRtcEngineEventHandler.AudioVolumeInfo[] speakers, int totalVolume);
}
