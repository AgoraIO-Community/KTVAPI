package io.agora.ktvdemo.ui

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import io.agora.karaoke_view.v11.KaraokeView
import io.agora.karaoke_view_ex.constants.DownloadError
import io.agora.karaoke_view_ex.downloader.LyricsFileDownloader
import io.agora.karaoke_view_ex.downloader.LyricsFileDownloaderCallback
import io.agora.ktvapi.*
import io.agora.ktvdemo.BuildConfig
import io.agora.ktvdemo.MyApplication
import io.agora.ktvdemo.R
import io.agora.ktvdemo.api.CloudApiManager
import io.agora.ktvdemo.databinding.FragmentLivingBinding
import io.agora.ktvdemo.rtc.RtcEngineController
import io.agora.ktvdemo.utils.KeyCenter
import io.agora.ktvdemo.utils.SongSourceType
import io.agora.ktvdemo.utils.TokenGenerator
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcConnection
import java.io.File
import java.util.concurrent.Executors

/*
 * K 歌体验页面
 */
class LivingFragment : BaseFragment<FragmentLivingBinding>() {

    /*
     * 歌词组件的 view
     */
    private var karaokeView: KaraokeView? = null

    /*
     * KTVAPI 实例
     */
    private lateinit var ktvApi: KTVApi

    /*
     * KTVAPI 事件
     */
    private val ktvApiEventHandler = object : IKTVApiEventHandler() {}

    private val scheduledThreadPool = Executors.newScheduledThreadPool(5)

    override fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentLivingBinding {
        return FragmentLivingBinding.inflate(inflater)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sceneName =
            if (KeyCenter.isNormalChorus) getString(R.string.app_normal_ktvapi_tag) else getString(R.string.app_giant_ktvapi_tag)
        val suffix = if (KeyCenter.songSourceType==SongSourceType.Mcc) "Mcc" else "Local"
        binding?.tvChorusScene?.text = "$sceneName Channel:${KeyCenter.channelId} $suffix"

        // 大合唱模式下主唱需要启动云端合流
        if (KeyCenter.isBroadcaster && !KeyCenter.isNormalChorus) {
            TokenGenerator.generateToken("${KeyCenter.channelId}_ad", CloudApiManager.outputUid.toString(),
                TokenGenerator.TokenGeneratorType.token007, TokenGenerator.AgoraTokenType.rtc,
                success = { outputToken ->
                    // uid
                    val inputRtcUid = 0
                    TokenGenerator.generateToken(KeyCenter.channelId, inputRtcUid.toString(),
                        TokenGenerator.TokenGeneratorType.token007, TokenGenerator.AgoraTokenType.rtc,
                        success = { inputToken ->
                            scheduledThreadPool.execute {
                                CloudApiManager.getInstance().fetchStartCloud(
                                    KeyCenter.channelId,
                                    inputRtcUid,
                                    inputToken,
                                    outputToken
                                )
                            }
                        },
                        failure = {
                            toast("云端合流启动失败, token获取失败")
                        }
                    )
                },
                failure = {
                    toast("云端合流启动失败, token获取失败")
                }
            )
        }

        initView()
        initKTVApi()
        joinChannel()

        // 设置麦克风初始状态，主唱默认开麦
        if (KeyCenter.isBroadcaster) {
            ktvApi.muteMic(false)
        }
//        loadMusic()
    }

    override fun onDestroy() {
        ktvApi.switchSingerRole(KTVSingRole.Audience, null)
        ktvApi.removeEventHandler(ktvApiEventHandler)
        ktvApi.release()
        RtcEngineController.rtcEngine.leaveChannel()
        super.onDestroy()
    }

    private fun initView() {
        binding?.apply {
            karaokeView = KaraokeView(lyricsView, scoringView)

            // 退出场景
            btnClose.setOnClickListener {
                ktvApi.switchSingerRole(KTVSingRole.Audience, null)
                ktvApi.removeEventHandler(ktvApiEventHandler)
                ktvApi.release()
                RtcEngineController.rtcEngine.leaveChannel()
                if (KeyCenter.isBroadcaster && !KeyCenter.isNormalChorus) {
                    scheduledThreadPool.execute {
                        CloudApiManager.getInstance().fetchStopCloud()
                    }
                }
                findNavController().popBackStack()
            }
            if (KeyCenter.isBroadcaster) {
                tvSinger.text = getString(R.string.app_lead_singer)
            } else {
                tvSinger.text = getString(R.string.app_audience)
            }

            // 加入合唱
            btJoinChorus.setOnClickListener {
                if (KeyCenter.isBroadcaster) {
                    toast(getString(R.string.app_no_premission))
                } else {
                    val songCode = KeyCenter.mccSongCode
                    // 使用声网版权中心歌单
                    val musicConfiguration = KTVLoadMusicConfiguration(
                        songCode.toString(), // 需要传入唯一的歌曲id，demo 简化逻辑传了songCode
                        KeyCenter.LeadSingerUid,
                        KTVLoadMusicMode.LOAD_MUSIC_ONLY,
                        false,
                    )
                    ktvApi.loadMusic(songCode, musicConfiguration, object : IMusicLoadStateListener {
                        override fun onMusicLoadSuccess(songCode: Long, lyricUrl: String) {
                            Log.d("Music", "onMusicLoadSuccess, songCode: $songCode, lyricUrl: $lyricUrl")
                            // 切换身份为合唱者
                            ktvApi.switchSingerRole(KTVSingRole.CoSinger, object : ISwitchRoleStateListener {
                                override fun onSwitchRoleSuccess() {
                                    mainHandler.post {
                                        toast("加入合唱成功，自动开麦")
                                        ktvApi.muteMic(false)
                                        btMicStatus.text = "麦克风开"
                                        tvSinger.text = getString(R.string.app_co_singer)
                                        btJoinChorus.isActivated = true
                                        btMicOn.isActivated = true
                                        btMicOff.isActivated = false
                                    }
                                }

                                override fun onSwitchRoleFail(reason: SwitchRoleFailReason) {
                                    mainHandler.post {
                                        toast("加入合唱失败")
                                        btJoinChorus.isActivated = false
                                    }
                                }
                            })

                        }

                        override fun onMusicLoadFail(songCode: Long, reason: KTVLoadMusicFailReason) {
                            Log.d("Music", "onMusicLoadFail, songCode: $songCode, reason: $reason")
                        }

                        override fun onMusicLoadProgress(
                            songCode: Long,
                            percent: Int,
                            status: MusicLoadStatus,
                            msg: String?,
                            lyricUrl: String?
                        ) {
                            Log.d("Music", "onMusicLoadProgress, songCode: $songCode, percent: $percent")
                            mainHandler.post {
                                binding?.btLoadProgress?.text = "下载进度：$percent%"
                            }
                        }
                    })
                }
            }

            // 退出合唱
            btLeaveChorus.setOnClickListener {
                if (KeyCenter.isBroadcaster) {
                    toast(getString(R.string.app_no_premission))
                } else {
                    ktvApi.switchSingerRole(KTVSingRole.Audience, null)
                    tvSinger.text = getString(R.string.app_audience)
                    toast("退出合唱成功")
                    btJoinChorus.isActivated = false
                }
            }

            // 开原唱：仅领唱和合唱者可以做这项操作
            btOriginal.setOnClickListener {
                ktvApi.switchAudioTrack(AudioTrackMode.YUAN_CHANG)

                btOriginal.isActivated = true
                btAcc.isActivated = false
                btDaoChang.isActivated = false
            }

            // 开伴奏：仅领唱和合唱者可以做这项操作
            btAcc.setOnClickListener {
                ktvApi.switchAudioTrack(AudioTrackMode.BAN_ZOU)

                btOriginal.isActivated = false
                btAcc.isActivated = true
                btDaoChang.isActivated = false
            }

            // 开导唱：仅领唱可以做这项操作，开启后领唱本地听到歌曲原唱，但观众听到仍为伴奏
            btDaoChang.setOnClickListener {
                ktvApi.switchAudioTrack(AudioTrackMode.DAO_CHANG)

                btOriginal.isActivated = false
                btAcc.isActivated = false
                btDaoChang.isActivated = true
            }

            // 加载音乐
            btLoadMusic.setOnClickListener {
                loadMusic()
                btLoadMusic.isActivated = true
                btRemoveMusic.isActivated = false
            }

            // 取消加载歌曲并删除本地歌曲缓存
            btRemoveMusic.setOnClickListener {
                // 模拟移除
                ktvApi.switchSingerRole(KTVSingRole.Audience, null)
                btLoadMusic.isActivated = false
                btRemoveMusic.isActivated = true
                lyricsView.reset()
                isLyricDataSet = false
            }

            // 开麦
            btMicOn.setOnClickListener {
                ktvApi.muteMic(false)
                btMicStatus.text = "麦克风开"

                btMicOn.isActivated = true
                btMicOff.isActivated = false
            }

            // 关麦
            btMicOff.setOnClickListener {
                ktvApi.muteMic(true)
                btMicStatus.text = "麦克风关"

                btMicOn.isActivated = false
                btMicOff.isActivated = true
            }

            // 设置麦克风初始状态
            if (KeyCenter.isBroadcaster) {
                btMicStatus.text = "麦克风开"
            } else {
                btMicStatus.text = "麦克风关"
            }

            btPause.setOnClickListener {
                if (KeyCenter.isBroadcaster) {
                    ktvApi.pauseSing()
                    btPlay.isActivated = false
                    btPause.isActivated = true
                } else {
                    toast(getString(R.string.app_no_premission))
                }
            }

            btPlay.setOnClickListener {
                if (KeyCenter.isBroadcaster) {
                    btPlay.isActivated = true
                    btPause.isActivated = false
                    ktvApi.resumeSing()
                } else {
                    toast(getString(R.string.app_no_premission))
                }
            }
        }
    }

    // 防止歌词没设置，直接 set pitch
    @Volatile
    private var isLyricDataSet = false

    /*
     * 初始化 KTVAPI
     */
    private fun initKTVApi() {
        // ------------------ 初始化内容中心 ------------------
        KTVApi.debugMode = true
        KTVApi.mccDomain = "api-test.agora.io"
        if (KeyCenter.isNormalChorus) {
            // 创建普通合唱ktvapi实例
            ktvApi = createKTVApi(
                KTVApiConfig(
                    appId = BuildConfig.AGORA_APP_ID,
                    rtmToken = RtcEngineController.rtmToken,
                    engine = RtcEngineController.rtcEngine,
                    channelName = KeyCenter.channelId,
                    localUid = KeyCenter.localUid,
                    chorusChannelName = "${KeyCenter.channelId}_ex",
                    chorusChannelToken = RtcEngineController.chorusChannelRtcToken,
                    maxCacheSize = 10,
                    type = KTVType.Normal,
                    musicType = if (KeyCenter.songSourceType == SongSourceType.Mcc) KTVMusicType.SONG_CODE else KTVMusicType.SONG_URL
                )
            )
        } else {
            // 创建大合唱ktvapi实例
            ktvApi = createKTVGiantChorusApi(
                KTVGiantChorusApiConfig(
                    appId = BuildConfig.AGORA_APP_ID,
                    rtmToken = RtcEngineController.rtmToken,
                    engine = RtcEngineController.rtcEngine,
                    localUid = KeyCenter.localUid,
                    audienceChannelName = KeyCenter.channelId + "_ad",
                    audienceChannelToken = RtcEngineController.audienceChannelToken,
                    chorusChannelName = KeyCenter.channelId,
                    chorusChannelToken = RtcEngineController.chorusChannelRtcToken,
                    musicStreamUid = 2023,
                    musicStreamToken = RtcEngineController.musicStreamToken,
                    maxCacheSize = 10,
                    musicType = if (KeyCenter.songSourceType == SongSourceType.Mcc) KTVMusicType.SONG_CODE else KTVMusicType.SONG_URL
                )
            )
        }
        // 注册 ktvapi 事件
        ktvApi.addEventHandler(ktvApiEventHandler)
        // 设置歌词组件
        ktvApi.setLrcView(object : ILrcView {

            override fun onUpdatePitch(pitch: Float?) {
                pitch?.let {
                    karaokeView?.setPitch(it)
                }
            }

            override fun onUpdateProgress(progress: Long?) {
                progress?.let {
                    karaokeView?.setProgress(it)
                }
            }

            override fun onDownloadLrcData(url: String?) {
                url?.let {
                    dealDownloadLrc(it)
                }
            }

            override fun onHighPartTime(highStartTime: Long, highEndTime: Long) {
            }
        })
    }

    /*
     * 加入 RTC 频道
     */
    private fun joinChannel() {
        val channelMediaOptions = ChannelMediaOptions().apply {
            autoSubscribeAudio = true
            autoSubscribeVideo = true
            publishCameraTrack = false
            publishMicrophoneTrack = KeyCenter.isBroadcaster
            clientRoleType =
                if (KeyCenter.isBroadcaster) io.agora.rtc2.Constants.CLIENT_ROLE_BROADCASTER
                else io.agora.rtc2.Constants.CLIENT_ROLE_AUDIENCE
        }

        if (KeyCenter.isNormalChorus) {
            // 普通合唱或独唱加入频道
            RtcEngineController.rtcEngine.joinChannel(
                RtcEngineController.audienceChannelToken,
                KeyCenter.channelId,
                KeyCenter.localUid,
                channelMediaOptions
            )
        } else {
            // 大合唱加入频道
            // 大合唱加入频道
            RtcEngineController.rtcEngine.joinChannelEx(
                RtcEngineController.audienceChannelToken,
                RtcConnection(KeyCenter.channelId + "_ad", KeyCenter.localUid),
                channelMediaOptions,
                object : IRtcEngineEventHandler() {
                    override fun onStreamMessage(uid: Int, streamId: Int, data: ByteArray?) {
                        (ktvApi as KTVGiantChorusApiImpl).setAudienceStreamMessage(uid, streamId, data)
                    }

                    override fun onAudioMetadataReceived(uid: Int, data: ByteArray?) {
                        super.onAudioMetadataReceived(uid, data)
                        (ktvApi as KTVGiantChorusApiImpl).setAudienceAudioMetadataReceived(uid, data)
                    }
                }
            )
            RtcEngineController.rtcEngine.setParametersEx(
                "{\"rtc.use_audio4\": true}",
                RtcConnection(KeyCenter.channelId + "_ad", KeyCenter.localUid)
            )
        }

        // 加入频道后需要更新数据传输通道
        ktvApi.renewInnerDataStreamId()
    }

    /*
     * 加载、播放音乐
     */
    private fun loadMusic() {
        if (KeyCenter.songSourceType == SongSourceType.Mcc) {
            // 使用声网版权中心歌单
            val songCode = KeyCenter.mccSongCode
            // 使用声网版权中心歌单
            val musicConfiguration = KTVLoadMusicConfiguration(
                songCode.toString(), // 需要传入唯一的歌曲id，demo 简化逻辑传了songCode
                KeyCenter.LeadSingerUid,
                if (KeyCenter.isBroadcaster) KTVLoadMusicMode.LOAD_MUSIC_AND_LRC else KTVLoadMusicMode.LOAD_LRC_ONLY,
            )
            ktvApi.loadMusic(songCode, musicConfiguration, object : IMusicLoadStateListener {
                override fun onMusicLoadSuccess(songCode: Long, lyricUrl: String) {
                    Log.d("Music", "onMusicLoadSuccess, songCode: $songCode, lyricUrl: $lyricUrl")
                    if (KeyCenter.isBroadcaster) {
                        ktvApi.switchSingerRole(KTVSingRole.LeadSinger, object : ISwitchRoleStateListener {
                            override fun onSwitchRoleSuccess() {

                                // 加载成功开始播放音乐
                                ktvApi.startSing(songCode, 0)
                            }

                            override fun onSwitchRoleFail(reason: SwitchRoleFailReason) {

                            }
                        })
                    }
                }

                override fun onMusicLoadFail(songCode: Long, reason: KTVLoadMusicFailReason) {
                    Log.d("Music", "onMusicLoadFail, songCode: $songCode, reason: $reason")
                }

                override fun onMusicLoadProgress(
                    songCode: Long,
                    percent: Int,
                    status: MusicLoadStatus,
                    msg: String?,
                    lyricUrl: String?
                ) {
                    Log.d("Music", "onMusicLoadProgress, songCode: $songCode, percent: $percent")
                    mainHandler.post {
                        binding?.btLoadProgress?.text = "下载进度：$percent%"
                    }
                }
            })
        } else {
            // 使用本地音乐文件
            val musicConfiguration = KTVLoadMusicConfiguration(
                KeyCenter.mccSongCode.toString(), // 需要传入唯一的歌曲id，demo 简化逻辑传了songCode
                KeyCenter.LeadSingerUid,
                KTVLoadMusicMode.LOAD_NONE
            )
            val songPath = requireActivity().filesDir.absolutePath + File.separator
            val songName = "不如跳舞"
            ktvApi.loadMusic("$songPath$songName.mp4", musicConfiguration)
            val fileLrc = File("$songPath$songName.xml")
            val lyricsModel = KaraokeView.parseLyricsData(fileLrc)
            karaokeView?.lyricsData = lyricsModel
            if (KeyCenter.isBroadcaster) {
                ktvApi.switchSingerRole(KTVSingRole.LeadSinger, object : ISwitchRoleStateListener {
                    override fun onSwitchRoleSuccess() {
                        ktvApi.startSing("$songPath$songName.mp4", 0)
                    }

                    override fun onSwitchRoleFail(reason: SwitchRoleFailReason) {

                    }
                })
            }
        }

    }

    private fun dealDownloadLrc(lrcUrl: String) {
        val context: Context = MyApplication.app()
        LyricsFileDownloader.getInstance(context)
            .setLyricsFileDownloaderCallback(object : LyricsFileDownloaderCallback {
                override fun onLyricsFileDownloadProgress(requestId: Int, progress: Float) {}

                override fun onLyricsFileDownloadCompleted(requestId: Int, fileData: ByteArray, error: DownloadError?) {
                    if (error == null) {
                        val lyricsModel = KaraokeView.parseLyricsData(fileData)
                        if (lyricsModel == null) {
                            Toast.makeText(context, "Unexpected parseLyricsData", Toast.LENGTH_SHORT).show()
                            return
                        }
                        karaokeView?.let {
                            karaokeView?.lyricsData = lyricsModel
                        }
                    } else {
                        error.message?.let {
                            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            })
        LyricsFileDownloader.getInstance(context).download(lrcUrl)
    }
}