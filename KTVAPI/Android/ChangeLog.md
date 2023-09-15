1、KTVApiConfig
新增参数musicType: 音乐类型，默认为mcc曲库
/**
* 初始化KTVApi的配置
* @param appId 用来初始化 Mcc Engine
* @param rtmToken 创建 Mcc Engine 需要
* @param engine RTC engine 对象
* @param channelName 频道号，子频道名以基于主频道名 + "_ex" 固定规则生成频道号
* @param localUid 创建 Mcc engine 和 加入子频道需要用到
* @param chorusChannelName 子频道名 加入子频道需要用到
* @param chorusChannelToken 子频道token 加入子频道需要用到
* @param maxCacheSize 最大缓存歌曲数
* @param type KTV场景
* @param musicType 音乐类型，默认为mcc版权歌单
  */
  data class KTVApiConfig constructor(
  val appId: String,
  val rtmToken: String,
  val engine: RtcEngine,
  val channelName: String,
  val localUid: Int,
  val chorusChannelName: String,
  val chorusChannelToken: String,
  val maxCacheSize: Int = 10,
  val type: KTVType = KTVType.Normal,
  val musicType: KTVSongType = KTVSongType.SONG_CODE
  )


2、KTVMusicType
新增类型
/**
* KTV歌曲类型
* @param SONG_CODE mcc版权歌单songCode
* @param SONG_URL 本地歌曲地址url
  */
  enum class KTVMusicType(val value: Int) {
  SONG_CODE(0),
  SONG_URL(1)
  }


3、enableProfessionalStreamerMode
新增接口, 专业模式开关, 开启专业模式后, 场景化api会针对耳机/外放采用不同的音频配置
/**
  * 开启关闭专业模式
  */
  fun enableProfessionalStreamerMode(enable: Boolean)


4、createKTVApi
新增接口, 创建ktvapi实例
/**
* 创建 KTVApi 实例
  */
  fun createKTVApi(): KTVApi = KTVApiImpl()
  创建 KTVAPI 模块由:
  val ktvApiProtocol = KTVApiImpl()  
  变更为:
  val ktvApiProtocol = createKTVApi()