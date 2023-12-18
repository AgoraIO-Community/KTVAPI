package io.agora.ktvdemo.ui

import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import io.agora.karaoke_view.v11.KaraokeView
import io.agora.ktvapi.*
import io.agora.ktvdemo.BuildConfig
import io.agora.ktvdemo.MyApplication
import io.agora.ktvdemo.R
import io.agora.ktvdemo.databinding.FragmentLivingBinding
import io.agora.ktvdemo.rtc.RtcEngineController
import io.agora.ktvdemo.utils.DownloadUtils
import io.agora.ktvdemo.utils.KeyCenter
import io.agora.ktvdemo.utils.ZipUtils
import io.agora.rtc2.ChannelMediaOptions
import java.io.File

class LivingFragment : BaseFragment<FragmentLivingBinding>() {

    private var karaokeView: KaraokeView? = null

    private val ktvApi: KTVApi by lazy {
        createKTVApi()
    }

    private val ktvApiEventHandler = object : IKTVApiEventHandler() {}

    override fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentLivingBinding {
        return FragmentLivingBinding.inflate(inflater)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initView()
        initKTVApi()
        joinChannel()
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
            karaokeView = KaraokeView(lyricsView,null)
            btnClose.setOnClickListener {
                ktvApi.switchSingerRole(KTVSingRole.Audience, null)
                ktvApi.removeEventHandler(ktvApiEventHandler)
                ktvApi.release()
                RtcEngineController.rtcEngine.leaveChannel()
                findNavController().popBackStack()
            }
            if (KeyCenter.isLeadSinger()) {
                tvSinger.text = getString(R.string.app_lead_singer)
            } else if (KeyCenter.isCoSinger()) {
                tvSinger.text = getString(R.string.app_co_singer)
            } else {
                tvSinger.text = getString(R.string.app_audience)
            }

            btJoinChorus.setOnClickListener {
                if (KeyCenter.isLeadSinger()) {
                    Toast.makeText(MyApplication.app(), R.string.app_no_premission, Toast.LENGTH_SHORT).show()
                } else {
                    ktvApi.switchSingerRole(KTVSingRole.CoSinger, null)
                }
            }

            btLeaveChorus.setOnClickListener {
                if (KeyCenter.isLeadSinger()) {
                    Toast.makeText(MyApplication.app(), R.string.app_no_premission, Toast.LENGTH_SHORT).show()
                } else {
                    ktvApi.switchSingerRole(KTVSingRole.Audience, null)
                }
            }

            btOriginal.setOnClickListener {
                if (KeyCenter.isLeadSinger()) {
                    ktvApi.getMediaPlayer().selectMultiAudioTrack(0, 0)
                } else if (KeyCenter.isCoSinger()) {
                    ktvApi.getMediaPlayer().selectAudioTrack(0)
                } else {
                    Toast.makeText(MyApplication.app(), R.string.app_no_premission, Toast.LENGTH_SHORT).show()
                }
            }

            btAcc.setOnClickListener {
                if (KeyCenter.isLeadSinger()) {
                    ktvApi.getMediaPlayer().selectMultiAudioTrack(1, 1)
                } else if (KeyCenter.isCoSinger()) {
                    ktvApi.getMediaPlayer().selectAudioTrack(1)
                } else {
                    Toast.makeText(MyApplication.app(), R.string.app_no_premission, Toast.LENGTH_SHORT).show()
                }
            }

            btDaoChang.setOnClickListener {
                if (KeyCenter.isLeadSinger()) {
                    ktvApi.getMediaPlayer().selectMultiAudioTrack(0, 1)
                } else if (KeyCenter.isCoSinger()) {
                    Toast.makeText(MyApplication.app(), R.string.app_no_premission, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(MyApplication.app(), R.string.app_no_premission, Toast.LENGTH_SHORT).show()
                }
            }

            btLoadMusic.setOnClickListener {
                if (KeyCenter.isMcc) {
                    val musicConfiguration = KTVLoadMusicConfiguration(
                        KeyCenter.songCode.toString(), false, KeyCenter.LeadSingerUid,
                        if (KeyCenter.isAudience()) KTVLoadMusicMode.LOAD_LRC_ONLY else KTVLoadMusicMode.LOAD_MUSIC_AND_LRC
                    )
                    ktvApi.loadMusic(KeyCenter.songCode, musicConfiguration, object : IMusicLoadStateListener {
                        override fun onMusicLoadSuccess(songCode: Long, lyricUrl: String) {
                            Log.d("Music", "onMusicLoadSuccess, songCode: $songCode, lyricUrl: $lyricUrl")
                            if (KeyCenter.isLeadSinger()) {
                                ktvApi.switchSingerRole(KTVSingRole.LeadSinger, object : ISwitchRoleStateListener {
                                    override fun onSwitchRoleSuccess() {
                                        ktvApi.startSing(KeyCenter.songCode, 0)
                                    }

                                    override fun onSwitchRoleFail(reason: SwitchRoleFailReason) {

                                    }
                                })
                            } else if (KeyCenter.isCoSinger()) {
                                ktvApi.switchSingerRole(KTVSingRole.CoSinger, object : ISwitchRoleStateListener {
                                    override fun onSwitchRoleSuccess() {

                                    }

                                    override fun onSwitchRoleFail(reason: SwitchRoleFailReason) {

                                    }
                                })
                            }
                        }

                        override fun onMusicLoadFail(songCode: Long, reason: KTVLoadSongFailReason) {
                            Log.d("Music", "onMusicLoadFail, songCode: $songCode, reason: $reason")
                        }

                        override fun onMusicLoadProgress(
                            songCode: Long,
                            percent: Int,
                            status: MusicLoadStatus,
                            msg: String?,
                            lyricUrl: String?
                        ) {
                            lyricsView.setLabelShownWhenNoLyrics("$percent%")
                            Log.d("Music", "onMusicLoadProgress, songCode: $songCode, percent: $percent")
                        }
                    })
                } else {
                    val musicConfiguration = KTVLoadMusicConfiguration(
                        KeyCenter.songCode.toString(), false, KeyCenter.LeadSingerUid, KTVLoadMusicMode.LOAD_NONE
                    )
                    val songPath = requireActivity().filesDir.absolutePath + File.separator
                    val songName = "成都"
                    ktvApi.loadMusic("$songPath$songName.mp3", musicConfiguration)
                    val fileLrc = File("$songPath$songName.xml")
                    val lyricsModel = KaraokeView.parseLyricsData(fileLrc)
                    karaokeView?.lyricsData = lyricsModel
                    if (KeyCenter.isLeadSinger()) {
                        ktvApi.switchSingerRole(KTVSingRole.LeadSinger, object : ISwitchRoleStateListener {
                            override fun onSwitchRoleSuccess() {
                                ktvApi.startSing("$songPath$songName.mp3", 0)
                            }

                            override fun onSwitchRoleFail(reason: SwitchRoleFailReason) {

                            }
                        })
                    } else if (KeyCenter.isCoSinger()) {
                        ktvApi.switchSingerRole(KTVSingRole.CoSinger, object : ISwitchRoleStateListener {
                            override fun onSwitchRoleSuccess() {

                            }

                            override fun onSwitchRoleFail(reason: SwitchRoleFailReason) {

                            }
                        })
                    }
                }
            }

            btRemoveMusic.setOnClickListener {
                if (KeyCenter.isMcc) {
                    ktvApi.removeMusic(KeyCenter.songCode)
                    lyricsView.reset()
                } else {
                    Toast.makeText(MyApplication.app(), R.string.app_no_premission, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun joinChannel() {
        val channelMediaOptions = ChannelMediaOptions().apply {
            autoSubscribeAudio = true
            clientRoleType = if (KeyCenter.isAudience()) io.agora.rtc2.Constants.CLIENT_ROLE_AUDIENCE
            else io.agora.rtc2.Constants.CLIENT_ROLE_BROADCASTER
            autoSubscribeVideo = true
            autoSubscribeAudio = true
            publishCameraTrack = false
            publishMicrophoneTrack = !KeyCenter.isAudience()
        }
        RtcEngineController.rtcEngine.joinChannel(
            RtcEngineController.rtcToken,
            KeyCenter.channelId,
            KeyCenter.localUid,
            channelMediaOptions
        )
    }

    private fun initKTVApi() {
        val ktvApiConfig = KTVApiConfig(
            BuildConfig.AGORA_APP_ID,
            RtcEngineController.rtmToken,
            RtcEngineController.rtcEngine,
            KeyCenter.channelId,
            KeyCenter.localUid,
            "${KeyCenter.channelId}_ex",
            RtcEngineController.chorusChannelRtcToken,
            10,
            KTVType.Normal,
            if (KeyCenter.isMcc) KTVMusicType.SONG_CODE else KTVMusicType.SONG_URL
        )
        ktvApi.initialize(ktvApiConfig)
        ktvApi.addEventHandler(ktvApiEventHandler)
        ktvApi.renewInnerDataStreamId()
        ktvApi.setLrcView(object : ILrcView {
            override fun onUpdatePitch(pitch: Float) {
            }

            override fun onUpdateProgress(progress: Long) {
                karaokeView?.setProgress(progress)
            }

            override fun onDownloadLrcData(url: String) {
                dealDownloadLrc(url)
            }

            override fun onHighPartTime(highStartTime: Long, highEndTime: Long) {
            }
        })
    }

    private fun dealDownloadLrc(url: String) {
        DownloadUtils.getInstance().download(requireContext(), url, object : DownloadUtils.FileDownloadCallback {
            override fun onSuccess(file: File) {
                if (file.name.endsWith(".zip")) {
                    ZipUtils.unzipOnlyPlainXmlFilesAsync(file.absolutePath,
                        file.absolutePath.replace(".zip", ""),
                        object : ZipUtils.UnZipCallback {
                            override fun onFileUnZipped(unZipFilePaths: MutableList<String>) {
                                var xmlPath = ""
                                for (path in unZipFilePaths) {
                                    if (path.endsWith(".xml")) {
                                        xmlPath = path
                                        break
                                    }
                                }

                                if (TextUtils.isEmpty(xmlPath)) {
                                    toast("The xml file not exist!")
                                    return
                                }

                                val xmlFile = File(xmlPath)
                                val lyricsModel = KaraokeView.parseLyricsData(xmlFile)

                                if (lyricsModel == null) {
                                    toast("Unexpected content from $xmlPath")
                                    return
                                }
                                karaokeView?.lyricsData = lyricsModel
                            }

                            override fun onError(e: Exception?) {
                                toast(e?.message ?: "UnZip xml error")
                            }

                        }
                    )
                } else {
                    val lyricsModel = KaraokeView.parseLyricsData(file)
                    if (lyricsModel == null) {
                        toast("Unexpected content from $file")
                        return
                    }
                    karaokeView?.lyricsData = lyricsModel
                }
            }

            override fun onFailed(exception: Exception?) {
                toast("download lrc  ${exception?.message}")
            }
        })
    }
}