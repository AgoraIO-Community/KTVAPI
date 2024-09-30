package io.agora.ktvapi.soul;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class Const {


    @Retention(RetentionPolicy.SOURCE)
    public @interface AudioPreset {
        int EffectPresetRoomAcousticsOFF = 0;///<  无音效 默认
        int EffectPresetRoomAcousticsKTV = 1;///<  ktv音效
        int EffectPresetRommAcousVocalConcer = 2;///<  演唱会
        int EffectPresetRommAcousStudio = 3;///<  录音棚
        int EffectPresetRoomAcousPhonograph = 4;///<  留声机
        int EffectPresetRoomAcousVirtualStereo = 5;///<  虚拟立体声，即 SDK 将单声道的音频渲染出双声道的音效。
        int EffectPresetRoomAcousSpatial = 6;///<  空旷
        int EffectPresetRoomAcousEthereal = 7;///<  空灵
        int EffectPresetRoomAcous3DVoice = 8;///<  3D 人声
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface KtvAudioPlayType {
        int playTypeEncryption = 1; //播放本地加密歌曲
        int playTypeNonEncrypted = 2; //播放本地未加密歌曲
        int playTypeOnline = 3; //播放在线歌曲
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface StreamMsgType {
        int AudioPlay = 0;
        int VideoPlay = 1;
    }

    //收到主唱单行的分数
    public final static String SENTENCE_SCORE = "sentenceScore";
    //收到主唱总分
    public final static String TOTAL_SCORE = "totalScore";
    //收到主唱当前的音准
    public final static String CURRENT_PITCH = "currentPitch";

    public static boolean isRtcDestroy = true;

    //KTV相关角色
    /**
     * 直播主播（默认）
     */
    public final static int KTV_ROLE_BROADCASTER = 1;
    /**
     * 观众
     */
    public final static int KTV_ROLE_AUDIENCE = 2;
    /**
     * 主唱
     */
    public final static int KTV_ROLE_LEADER_SINGER = 3;
    /**
     * 副唱
     */
    public final static int KTV_ROLE_ACCOMPANY_SINGER = 4;

    public final static String GAME_START_NAME = "special_rtc_game_";

    public final static int SUBSCRIBE_AUDIO_STREAM = 1;  //订阅音频流

    public final static int SUBSCRIBE_VIDEO_STREAM = 2; //订阅视频流

    public final static int SUBSCRIBE_BOTH_STREAM = 3;  ////订阅音视频流
}
