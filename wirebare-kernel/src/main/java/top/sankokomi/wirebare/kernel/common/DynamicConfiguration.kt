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

import android.os.SystemClock
import androidx.annotation.IntRange
import kotlin.math.min

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
     * @param max 最大带宽 单位：KB/s
     * @param timeout 超时时间，由于带宽限制缓存的数据包超过此时间后将被丢弃 单位：ms
     * */
    class Bandwidth(
        @IntRange(from = -1L)
        val max: Long = 128L,
        val timeout: Long = -1L
    ) {
        private var storedBytes = 0L
        private var lastUpdate = SystemClock.elapsedRealtimeNanos()
        private val maxBurstBytes = max * 1024

        fun nextCanTransmit(packetSize: Int): Long {
            if (max <= 0L) {
                // 未配置带宽上限，可以立即发送
                return 0L
            }
            val now = SystemClock.elapsedRealtimeNanos()
            val deltaSec = (now - lastUpdate) / 1_000_000_000.0
            storedBytes = min(maxBurstBytes, (storedBytes + deltaSec * max * 1024).toLong())
            lastUpdate = now
            if (storedBytes >= 0) {
                // 配额足够，可以立即发送
                storedBytes -= packetSize
                return 0L
            }
            // 计算要发送这个数据还需要等待多久
            return 1000 * (packetSize - storedBytes) / maxBurstBytes
        }

        fun checkTimeout(time: Long): Boolean {
            if (timeout <= 0L) return false
            return time + timeout > SystemClock.elapsedRealtime()
        }
    }

}