package cn.soulapp.android.lib.media.agroa;

/**
 * Author : walid
 * Date : 2019-05-15 15:18
 * Describe : Agroa 引擎配置
 */
public class AgroaEngineConfig {

    public int clientRole;
    public int videoProfile;
    public int uid;
    public String appId;
    public String channel;
    public String mNativeLibPath;

    public void reset() {
        channel = null;
    }

    AgroaEngineConfig() {

    }

}
