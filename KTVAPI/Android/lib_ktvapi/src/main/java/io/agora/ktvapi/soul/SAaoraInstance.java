package io.agora.ktvapi.soul;

import static io.agora.rtc2.Constants.AUDIO_EQUALIZATION_BAND_FREQUENCY;
import static io.agora.rtc2.Constants.AUDIO_REVERB_TYPE;
import static io.agora.rtc2.video.VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_15;
import static io.agora.rtc2.video.VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_ADAPTIVE;
import static io.agora.rtc2.video.VideoEncoderConfiguration.STANDARD_BITRATE;
import static io.agora.rtc2.video.VideoEncoderConfiguration.VD_640x480;

import android.content.Context;

import androidx.annotation.WorkerThread;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import cn.soul.insight.log.core.SLogKt;
import cn.soulapp.android.lib.media.rtc.SoulRtcEngine;
import io.agora.rtc2.ChannelMediaOptions;
import io.agora.rtc2.Constants;
import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.RtcEngineEx;
import io.agora.rtc2.video.VideoEncoderConfiguration;

/**
 * Author : walid
 * Date : 2019-05-16 15:39
 * Describe :
 */
public class SAaoraInstance {
    @Retention(RetentionPolicy.SOURCE)
    public @interface Type {
        String TYPE_BROADCAST = "TYPE_BROADCAST";//直播
        String TYPE_COMMUNICATION = "TYPE_COMMUNICATION";//连麦
    }

    private volatile boolean pushSteam = true;
    private WorkerThread workerThread;
    private VideoEncoderConfiguration.VideoDimensions videoDimensions = VD_640x480;

    public boolean isPushSteam() {
        return pushSteam;
    }

    public void setPushSteam(boolean pushSteam) {
        this.pushSteam = pushSteam;
    }

    public synchronized void initWorkerThread(Context context, int uid, String appId, String mNativeLibPath) {
        SLogKt.SLogApi.d("sl_rtcEngine", "--initWorkerThread---");
        if (workerThread == null) {
            workerThread = new WorkerThread(context, uid, appId, mNativeLibPath);
            workerThread.start();
            workerThread.waitForReady();
        }
        RtcEngine rtcEngine = workerThread.getRtcEngine();
        if (rtcEngine != null) {
            rtcEngine.setLogFilter(Constants.LOG_FILTER_INFO);
        }
//        workerThread.getRtcEngine()
//                .setLogFile(Environment.getExternalStorageDirectory() + "/agora-rtc.log");
    }

    public synchronized void deInitWorkerThread() {
        SLogKt.SLogApi.d("sl_rtcEngine", "--deInitWorkerThread---");
        if (workerThread == null) return;
        workerThread.exit();
        SLogKt.SLogApi.d("sl_rtcEngine", "--deInitWorkerThread-exit--");
        workerThread = null;
    }

    private SAaoraInstance() {
    }

    public static SAaoraInstance getInstance() {
        return SingletonHolder.instance;
    }

    private static class SingletonHolder {
        static SAaoraInstance instance = new SAaoraInstance();
    }

    public RtcEngineEx rtcEngine() {
        if (workerThread == null) return null;
        return (RtcEngineEx) workerThread.getRtcEngine();
    }

    public final WorkerThread getWorker() {
        return workerThread;
    }

    public final AgroaEngineConfig getConfig() {
        if (workerThread == null) return null;
        return workerThread.getEngineConfig();
    }

    public final ChannelMediaOptions getOptions() {
        if (workerThread == null) return null;
        return new ChannelMediaOptions();
    }

    public void setVideoDimensions(VideoEncoderConfiguration.VideoDimensions videoDimensions) {
        this.videoDimensions = videoDimensions;
    }

    public final void updateOption(ChannelMediaOptions mediaOptions) {
        RtcEngineEx rtcEngine = rtcEngine();
        if (rtcEngine == null){
            SLogKt.SLogApi.e("sl_rtcEngine", "updateOption rtcEngine=null");
            return;
        }
        rtcEngine.updateChannelMediaOptions(mediaOptions);
    }

    public final AgroaEngineEventHandler getEventHandler() {
        return workerThread.eventHandler();
    }

    // 加入频道
    public int joinChannel(String channel, String token, @SoulRtcEngine.Type String type) {
        SLogKt.SLogApi.d("sl_rtcEngine", "--joinChannel---channel : " + channel);
        RtcEngine rtcEngine = rtcEngine();
        if (rtcEngine == null) return -1;
        rtcEngine.setVideoEncoderConfiguration(new VideoEncoderConfiguration(
                new VideoEncoderConfiguration.VideoDimensions(352, 628),
                // VideoEncoderConfiguration.VD_640x360,
                VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_15, 500,
                VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_FIXED_PORTRAIT));
        return getWorker().joinChannel(channel, token, type, getConfig().uid);
    }

    // 加入频道
    public int joinRoomChannel(String channel, String token, @SoulRtcEngine.Type String type, boolean isMultiRoom) {
        SLogKt.SLogApi.d("sl_rtcEngine", "--joinChannel--channel : " + channel);
        RtcEngine rtcEngine = rtcEngine();
        if (rtcEngine == null) return -1;
        VideoEncoderConfiguration.VideoDimensions  dimensions;
        if (isMultiRoom){
            dimensions = new VideoEncoderConfiguration.VideoDimensions(960, 540);
        }else {
            dimensions = videoDimensions;
        }
        rtcEngine.setVideoEncoderConfiguration(new VideoEncoderConfiguration(
                dimensions,
                FRAME_RATE_FPS_15,
                STANDARD_BITRATE,
                ORIENTATION_MODE_ADAPTIVE));
        return getWorker().joinChannel(channel, token, type, getConfig().uid);
    }

    // 退出频道
    public void leaveChannel(RtcEngineHandler rtcEngineHandler) {
        SLogKt.SLogApi.d("sl_rtcEngine", "--leaveChannel---");
        pushSteam = false;
        if (getWorker() != null) {
            getEventHandler().clear();
            getEventHandler().removeEventHandler(rtcEngineHandler);
            getWorker().leaveChannel(getConfig().channel);
        }
    }

    public void changeVoice(@Const.Role int role) {
        switch (role) {
            case Const.Role.UNCLE:
                setCheckedValue(80, -15, 0, 6, 1, -4, 1, -10, -5, 3, 3, 0, 90, 43, -12, -12);
                break;
            case Const.Role.BOY:
                setCheckedValue(123, 15, 11, -3, -5, -7, -7, -9, -15, -15, -15, 0, 91, 44, 4, 2);
                break;
            case Const.Role.BAJIE:
                setCheckedValue(60, 12, -9, -9, 3, -3, 11, 1, -8, -8, -9, 34, 0, 39, -14, -8);
                break;
            case Const.Role.VACANT:
                setCheckedValue(100, -8, -8, 5, 13, 2, 12, -3, 7, -2, -10, 72, 9, 69, -17, -13);
                break;
            case Const.Role.GIANT:
                setCheckedValue(50, -15, 3, -9, -8, -6, -4, -3, -2, -1, 1, 76, 124, 78, 10, -9);
                break;
            case Const.Role.LOLITA:
                setCheckedValue(145, 10, 6, 1, 1, -6, 13, 7, -14, 13, -13, 0, 31, 44, -11, -7);
                break;
        }
    }

    private void setCheckedValue(int pitch,
                                 int e31,
                                 int e62,
                                 int e125,
                                 int e250,
                                 int e500,
                                 int e1k,
                                 int e2k,
                                 int e4k,
                                 int e8k,
                                 int e16k,
                                 int room,
                                 int delay,
                                 int strength,
                                 int dry,
                                 int wet) {
        RtcEngine rtcEngine = rtcEngine();
        if (rtcEngine == null) return;

        rtcEngine.setLocalVoicePitch(Math.max((pitch * 1.f / 100.f), 0.5f));

        rtcEngine.setLocalVoiceEqualization(AUDIO_EQUALIZATION_BAND_FREQUENCY.AUDIO_EQUALIZATION_BAND_31, e31);
        rtcEngine.setLocalVoiceEqualization(AUDIO_EQUALIZATION_BAND_FREQUENCY.AUDIO_EQUALIZATION_BAND_62, e62);
        rtcEngine.setLocalVoiceEqualization(AUDIO_EQUALIZATION_BAND_FREQUENCY.AUDIO_EQUALIZATION_BAND_125, e125);
        rtcEngine.setLocalVoiceEqualization(AUDIO_EQUALIZATION_BAND_FREQUENCY.AUDIO_EQUALIZATION_BAND_250, e250);
        rtcEngine.setLocalVoiceEqualization(AUDIO_EQUALIZATION_BAND_FREQUENCY.AUDIO_EQUALIZATION_BAND_500, e500);
        rtcEngine.setLocalVoiceEqualization(AUDIO_EQUALIZATION_BAND_FREQUENCY.AUDIO_EQUALIZATION_BAND_1K, e1k);
        rtcEngine.setLocalVoiceEqualization(AUDIO_EQUALIZATION_BAND_FREQUENCY.AUDIO_EQUALIZATION_BAND_2K, e2k);
        rtcEngine.setLocalVoiceEqualization(AUDIO_EQUALIZATION_BAND_FREQUENCY.AUDIO_EQUALIZATION_BAND_4K, e4k);
        rtcEngine.setLocalVoiceEqualization(AUDIO_EQUALIZATION_BAND_FREQUENCY.AUDIO_EQUALIZATION_BAND_8K, e8k);
        rtcEngine.setLocalVoiceEqualization(AUDIO_EQUALIZATION_BAND_FREQUENCY.AUDIO_EQUALIZATION_BAND_16K, e16k);

        rtcEngine.setLocalVoiceReverb(AUDIO_REVERB_TYPE.AUDIO_REVERB_ROOM_SIZE, room);
        rtcEngine.setLocalVoiceReverb(AUDIO_REVERB_TYPE.AUDIO_REVERB_WET_DELAY, delay);
        rtcEngine.setLocalVoiceReverb(AUDIO_REVERB_TYPE.AUDIO_REVERB_STRENGTH, strength);
        rtcEngine.setLocalVoiceReverb(AUDIO_REVERB_TYPE.AUDIO_REVERB_DRY_LEVEL, dry);
        rtcEngine.setLocalVoiceReverb(AUDIO_REVERB_TYPE.AUDIO_REVERB_WET_LEVEL, wet);
    }

}