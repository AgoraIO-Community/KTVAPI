package cn.soulapp.android.lib.media.zego.interfaces;

import java.nio.ByteBuffer;

import cn.soulapp.android.lib.media.IAudioPlayerCallBack;
import cn.soulapp.android.lib.media.SLMediaPlayerState;
import cn.soulapp.android.lib.media.zego.beans.PlayKTVParams;
import cn.soulapp.android.lib.media.zego.beans.StreamMessage;

public interface IAgoraKTVChorusHelper {
    void setAudioPlayerCallBack(IAudioPlayerCallBack audioPlayerCallBack);

    void setRoomCallBack(IRoomCallback iRoomCallback);

    void release();

    void switchSingerRole(int role);

    void playKTVEncryptAudio(PlayKTVParams params, Boolean isAccompanyDelayPositionChange);

    void stopAudio();

    void setNewLeadSinger(int uid, PlayKTVParams params, Boolean isAccompanyDelayPositionChange);

    void pauseAudio();

    void resumeAudio();

    void selectAudioTrack(int audioTrack);

    boolean isAudioPlaying();

    long getAudioDuration();

    long getAudioCurrentPosition();

    void audioSeekTo(long l);

    SLMediaPlayerState getAudioPlayerState();

    void setAudioDelay(int audioDelay);

    void onRecordAudioFrame(ByteBuffer buffer, long renderTimeMs);

    void enableMic(boolean enable);

    String getChorusToken();

    void setVideoVolume(int volume);

    void setVideoVolume(int playoutVolume, int publishSignalVolume);

    // ================== onStreamMessage ==================
    boolean onStreamMessage(int uid, int streamId, StreamMessage msg);
}
