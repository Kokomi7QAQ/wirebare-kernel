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
import top.sankokomi.wirebare.kernel.annotation.Experimental

@Experimental
class DynamicConfiguration {

    /**
     * 请求丢包概率
     * */
    @Volatile
    @IntRange(from = -1, to = 100)
    @Experimental
    var reqPacketLossProb: Int = -1

    /**
     * 响应丢包概率
     * */
    @Volatile
    @IntRange(from = -1, to = 100)
    @Experimental
    var rspPacketLossProb: Int = -1

    /**
     * 带宽计算间隔
     *
     * 单位：ms
     * */
    @Volatile
    @IntRange(from = 10L)
    @Experimental
    var bandwidthStatInterval: Long = 2000L

    /**
     * 请求最大带宽
     *
     * 单位：KB/s
     * */
    @Volatile
    @Experimental
    var reqBandwidthLimiter: BandwidthLimiter = BandwidthLimiter()

    /**
     * 响应最大带宽
     *
     * 单位：KB/s
     * */
    @Volatile
    @Experimental
    var rspBandwidthLimiter: BandwidthLimiter = BandwidthLimiter()

}