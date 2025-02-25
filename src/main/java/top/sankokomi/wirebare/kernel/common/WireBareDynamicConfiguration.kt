package top.sankokomi.wirebare.kernel.common

import androidx.annotation.IntRange

class WireBareDynamicConfiguration {

    /**
     * 模拟丢包概率
     * 0 表示不丢包，1 表示全丢
     * */
    @IntRange(from = 0, to = 100)
    var mockPacketLossProbability: Int = 0

}