package cn.soulapp.android.lib.media.agroa.interfaces;

import cn.soulapp.android.lib.media.agroa.AgoraKTVStreamMessage;
import io.agora.mediaplayer.Constants;

public interface IAgoraKTVSyncProcess {
    public void startSyncProcess();

    public void onStartPlay();

    public void stop();

    /**
     *
     * @return true代表是否拦截
     */
    public boolean onStreamMessage(int uid, int streamId, AgoraKTVStreamMessage msg);

    /**
     * 进度回调
     * @param currentPosition
     */
    public void onPositionChanged(long currentPosition);

    /**
     * 获取当前进度，用来解决MediaPlayer.getPlayPosition()比较耗性能
     */
    public long getMediaPlayerPosition();

    public void onPlayerStateChanged(Constants.MediaPlayerState mediaPlayerState, Constants.MediaPlayerError mediaPlayerError);

    /**
     * 设备延迟
     */
    public void setAudioDelay(int audioDelay);

    public long getNtpTime();

    /**
     * 获取播放状态
     */
    public Constants.MediaPlayerState getMediaPlayerState();
}
