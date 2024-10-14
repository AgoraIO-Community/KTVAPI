package io.agora.ktvdemo.utils

import io.agora.ktvapiex.KTVSingRole

object KeyCenter {

    /*
     * 主唱 uid，每个频道只能有一个主唱所以我们固定了主唱的 uid
     */
    const val LeadSingerUid = 2024

    const val songCode: Long = 40289835
    const val songCode2: Long = 89488966

    /*
     * 加入的频道名
     */
    var channelId: String = ""

    /*
     * 自己的 uid
     */
    var localUid: Int = 2024

    /*
     * 选择的歌曲类型
     */
    var isMcc: Boolean = true

    /*
     * 体验 KTVAPI 的类型， true为普通合唱、false为大合唱
     */
    var isNormalChorus: Boolean = true

    /*
     * 当前演唱中的身份
     */
    var role: KTVSingRole = KTVSingRole.LeadSinger
}