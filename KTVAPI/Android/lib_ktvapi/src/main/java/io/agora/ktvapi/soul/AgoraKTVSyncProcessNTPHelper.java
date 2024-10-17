package cn.soulapp.android.lib.media.zego;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;

import cn.soulapp.android.lib.media.Const;
import cn.soulapp.android.lib.media.zego.beans.StreamMessage;
import cn.soulapp.android.lib.media.zego.interfaces.IAgoraKTVSyncProcess;
import io.agora.mediaplayer.Constants;
import io.agora.mediaplayer.IMediaPlayer;
import io.agora.rtc2.RtcEngineEx;

/**
 * 声网KTV合唱bgm同步NTP方案帮助类
 * Created by 罗康辉 on 2022/11/21
 */
class AgoraKTVSyncProcessNTPHelper implements IAgoraKTVSyncProcess {
    private static final String TAG = "SyncProcess";
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private RtcEngineEx mRtcEngine;
    private IMediaPlayer mMediaPlayer;
    private int mRole;
    private long mChangePosition;
    private long mChangePositionTime;
    private int mAudioDelay;
    private Constants.MediaPlayerState mMediaPlayerState;

    public AgoraKTVSyncProcessNTPHelper(AgoraKTVConfig ktvConfig){
        this.mRole = ktvConfig.role;
        this.mRtcEngine = ktvConfig.rtcEngine;
        this.mMediaPlayer = ktvConfig.mediaPlayer;
    }


    @Override
    public void startSyncProcess() {

    }

    @Override
    public void onStartPlay() {

    }


    @Override
    public void stop(){
        mHandler.removeCallbacks(mTimeOutRunnable);
        mChangePosition = 0;
        mChangePositionTime = 0;
        mMediaPlayerState = null;
        Log.i(TAG, "stop");
    }

    /**
     *
     * @return true代表是否拦截
     */
    @Override
    public boolean onStreamMessage(int uid, int streamId, StreamMessage msg) {
        String type = msg.type;
        if (mRole == Const.KTV_ROLE_ACCOMPANY_SINGER) {//伴唱
            if (StreamMessage.TYPE_PLAY_STATUS.equals(type)){
                mHandler.removeCallbacks(mTimeOutRunnable);
                if (TextUtils.equals(msg.playerState, ""+Constants.MediaPlayerState.getValue(Constants.MediaPlayerState.PLAYER_STATE_PLAYING))){
                    mHandler.postDelayed(mTimeOutRunnable, 5000);//5s
                    chorusSync(msg);
                    if (mMediaPlayer.getState() == Constants.MediaPlayerState.PLAYER_STATE_OPEN_COMPLETED
                        || mMediaPlayer.getState() == Constants.MediaPlayerState.PLAYER_STATE_PAUSED) {
                        mMediaPlayer.play();
                        Log.i(TAG, "onStreamMessage playStatus play");
                    }
                }else if (TextUtils.equals(msg.playerState, ""+Constants.MediaPlayerState.getValue(Constants.MediaPlayerState.PLAYER_STATE_PAUSED))){
                    if (mMediaPlayer.getState() != Constants.MediaPlayerState.PLAYER_STATE_PAUSED) {
                        mMediaPlayer.pause();
                        Log.i(TAG, "onStreamMessage playStatus pause");
                    }
                }

                return false;
            }
        }

        return false;
    }

    @Override
    public void onPositionChanged(long currentPosition) {
        mChangePosition = currentPosition;
        mChangePositionTime = SystemClock.elapsedRealtime();
    }

    @Override
    public long getMediaPlayerPosition() {
        if (mChangePositionTime <= 0){
            return mMediaPlayer.getPlayPosition();
        }
        return mChangePosition + (SystemClock.elapsedRealtime() - mChangePositionTime);
    }

    @Override
    public void onPlayerStateChanged(Constants.MediaPlayerState mediaPlayerState, Constants.MediaPlayerError mediaPlayerError) {
        //状态切换重置获取当前进度的时间，防止暂停等操作后，getMediaPlayerPosition不对的问题
        mChangePositionTime = 0;
        mMediaPlayerState = mediaPlayerState;
    }

    @Override
    public void setAudioDelay(int audioDelay) {
        this.mAudioDelay = audioDelay;
    }

    //同步进度
    private void chorusSync(StreamMessage msg){
        try {
            if (TextUtils.isEmpty(msg.currentTimeStamp)){
                return;
            }
            if (TextUtils.isEmpty(msg.currentDuration)){
                return;
            }
            long audioDelay = this.mAudioDelay;
            long currentNtpTime = getNtpTime();//当前自己ntp时间
            long playPosition = getMediaPlayerPosition();//伴唱
            long distance = (currentNtpTime - playPosition + audioDelay) - (Long.parseLong(msg.currentTimeStamp)  - Long.parseLong(msg.currentDuration));

            if (Math.abs(distance) > 80){
                mMediaPlayer.seek(playPosition + distance);
                Log.i(TAG, "chorusSync seek distance="+distance+" ,playPosition="+playPosition+" ,leaderProcess="+msg.currentDuration + " ,audioDeviceDelay="+audioDelay);
            }
        }catch (Throwable e){
            //Log.e(TAG, "chorusSync msg:"+ GsonUtils.entityToJson(msg) + " error:" + Log.getStackTraceString(e));
        }

    }


    private Runnable mTimeOutRunnable = new Runnable() {
        @Override
        public void run() {
            if (mMediaPlayer != null) {
                mMediaPlayer.pause();
            }
        }
    };

    @Override
    public long getNtpTime(){
        long currentNtpTime = mRtcEngine.getNtpTimeInMs();
        if (currentNtpTime != 0) {
            //Ntp转换UTC时间
            return currentNtpTime - 2208988800L * 1000;
        } else {
            Log.e(TAG, "getNtpTimeInMs DeviceDelay is zero!!!");
            return System.currentTimeMillis();
        }
    }

    @Override
    public Constants.MediaPlayerState getMediaPlayerState() {
        return mMediaPlayerState;
    }
}
