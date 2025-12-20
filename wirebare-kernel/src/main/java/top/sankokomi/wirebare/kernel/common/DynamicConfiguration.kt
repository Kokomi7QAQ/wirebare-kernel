/*
 * MIT License
 *
 * Copyright (c) 2025 KokomiQAQ
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package top.sankokomi.wirebare.kernel.common

import androidx.annotation.IntRange

class DynamicConfiguration {

    /**
     * 请求丢包概率
     * */
    @Volatile
    @IntRange(from = -1, to = 100)
    var reqPacketLossProb: Int = -1

    /**
     * 响应丢包概率
     * */
    @Volatile
    @IntRange(from = -1, to = 100)
    var rspPacketLossProb: Int = -1

    /**
     * 丢包概率，与 [reqPacketLossProb] 和 [rspPacketLossProb] 互斥，
     *
     * 都设置时，优先生效 [reqPacketLossProb] 和 [rspPacketLossProb]
     * */
    @Volatile
    @IntRange(from = -1, to = 100)
    var packetLossProb: Int = -1

    /**
     * 请求最大带宽
     *
     * 单位：KB/s
     * */
    @Volatile
    var reqMaxBandwidth: Bandwidth = Bandwidth()

    /**
     * 响应最大带宽
     *
     * 单位：KB/s
     * */
    @Volatile
    var rspMaxBandwidth: Bandwidth = Bandwidth()

    /**
     * 最大带宽
     *
     * 单位：KB/s
     * */
    @Volatile
    var maxBandwidth: Bandwidth = Bandwidth()

    /**
     * @param max 最大带宽 单位：KB/s
     * @param timeout 超时时间，由于带宽限制缓存的数据包超过此时间后将被丢弃 单位：ms
     * */
    class Bandwidth(
        @IntRange(from = -1L)
        val max: Long = -1L,
        val timeout: Long = -1L
    )

}