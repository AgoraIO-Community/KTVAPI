package io.agora.ktvdemo

import android.util.Log
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig
import io.agora.rtc2.RtcEngineEx

data class IChannelEventListener constructor(
    var onChannelJoined: ((channel: String, uid: Int) -> Unit)? = null,
    var onUserJoined: ((uid: Int) -> Unit)? = null,
    var onUserOffline: ((uid: Int) -> Unit)? = null,
)

object RtcEngineController {

    private var innerRtcEngine: RtcEngineEx? = null

    var eventListener: IChannelEventListener? = null
        set(value) {
            field = value
        }

    val rtcEngine: RtcEngineEx
        get() {
            if (innerRtcEngine == null) {
                val config = RtcEngineConfig()
                config.mContext = MyApplication.app()
                config.mAppId = BuildConfig.AGORA_APP_ID
                config.mEventHandler = object : IRtcEngineEventHandler() {
                    override fun onError(err: Int) {
                        super.onError(err)
                        Log.d(
                            "RtcEngineController",
                            "Rtc Error code:$err, msg:" + RtcEngine.getErrorDescription(err)
                        )
                    }

                    override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
                        super.onJoinChannelSuccess(channel, uid, elapsed)
                        eventListener?.onChannelJoined?.invoke(channel, uid)
                    }
                }
                innerRtcEngine = (RtcEngine.create(config) as RtcEngineEx).apply {
                    enableVideo()
                }
            }
            return innerRtcEngine!!
        }
}