package cn.soulapp.android.lib.media.agroa;

import java.io.Serializable;

public class AgoraKTVStreamMessage implements Serializable {

    public static final String TYPE_PLAY_STATUS = "playStatus";//播放器状态
    public static final String TYPE_CUSTOM = "custom";//业务流

    public String type;
    public String data;//新数据格式，https://wiki.soulapp-inc.cn/pages/viewpage.action?pageId=236956038

    //进度同步相关数据
    public String currentDuration;//当前播放进度
    public String audioDelay;//硬件等延迟声音
    public String duration;//音频总时间长
    public String playerState;//播放状态
    public String audioUniId;//音频唯一id
    public String currentTimeStamp;//当前NTP时间
}
