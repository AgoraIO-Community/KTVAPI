package io.agora.ktvapi.soul;

import cn.soulapp.android.lib.media.agroa.AgroaEngineConfig;
import io.agora.mediaplayer.IMediaPlayer;
import io.agora.rtc2.RtcEngineEx;

/**
 * ktv合唱需要配置数据
 * Created by 罗康辉 on 2022/11/15
 */
class AgoraKTVConfig {
    public int role;
    public RtcEngineEx rtcEngine;
    public IMediaPlayer mediaPlayer;
    public AgroaEngineConfig agroaEngineConfig;
    public String chorusToken; //主唱人声流token
    public String chorusChannelId;//主唱人声流channelId
    public int leaderUid;//主唱uid
    public int syncSteamId;//同步进度通道id
    public int playbackSignalVolume;//收流音量
}