package io.agora.ktvdemo.utils

object KeyCenter {

    /*
     * 主唱 uid，每个频道只能有一个主唱所以我们固定了主唱的 uid
     */
    const val LeadSingerUid = 2024

    val mccSongCode: Long  get() = 6625526605291650  // 6654550265524810
    val mccExSongCode: Long get() = 89488966 //40289835

    /*
     * 加入的频道名
     */
    var channelId: String = ""

    /*
     * 自己的 uid
     */
    var localUid: Int = 2024

    /*
     * 体验 KTVAPI 的类型， true为普通合唱、false为大合唱
     */
    var isNormalChorus: Boolean = true

    /*
     * 当前演唱中的身份
     */
    var isBroadcaster: Boolean = false

    /**
     * 歌曲来源
     */
    var songSourceType: SongSourceType = SongSourceType.Local
}

/**
 * Song source type
 *
 * @constructor Create empty Song source type
 */
enum class SongSourceType{
    Local, // 本地歌曲
    Mcc,   // Agora MCC
    MccEx  // Agora MCC EX
}