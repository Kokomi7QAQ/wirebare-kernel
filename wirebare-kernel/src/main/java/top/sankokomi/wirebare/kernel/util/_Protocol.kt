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

package top.sankokomi.wirebare.kernel.util

import top.sankokomi.wirebare.kernel.net.IntIPv6
import top.sankokomi.wirebare.kernel.net.IPVersion


/**
 * 将 [Int] 所代表的 IP 地址转换为 %s:%s:%s:%s 的形式
 * */
internal val Int.convertIPv4ToString: String
    get() = String.format(
        "%s.%s.%s.%s",
        this shr 24 and 0xFF,
        this shr 16 and 0xFF,
        this shr 8 and 0xFF,
        this and 0xFF
    )

internal val String.convertIPv4ToInt: Int
    get() = split(".").let { numbers ->
        try {
            return@let (numbers[0].toInt() and 0xFF shl 24) or
                    (numbers[1].toInt() and 0xFF shl 16) or
                    (numbers[2].toInt() and 0xFF shl 8) or
                    (numbers[3].toInt() and 0xFF)
        } catch (_: Exception) {
            throw IllegalArgumentException("IPv4 address format failed $this")
        }
    }

internal val IntIPv6.convertIPv6ToString: String
    get() = String.format(
        "%s:%s:%s:%s:%s:%s:%s:%s",
        (this.high64 shr 48 and 0xFFFF).toString(16),
        (this.high64 shr 32 and 0xFFFF).toString(16),
        (this.high64 shr 16 and 0xFFFF).toString(16),
        (this.high64 and 0xFFFF).toString(16),
        (this.low64 shr 48 and 0xFFFF).toString(16),
        (this.low64 shr 32 and 0xFFFF).toString(16),
        (this.low64 shr 16 and 0xFFFF).toString(16),
        (this.low64 and 0xFFFF).toString(16)
    )

internal val String.convertIPv6ToInt: IntIPv6
    get() = split(":").let { numbers ->
        try {
            return@let IntIPv6(
                (numbers[0].toLong(16) and 0xFFFF shl 48) or
                        (numbers[1].toLong(16) and 0xFFFF shl 32) or
                        (numbers[2].toLong(16) and 0xFFFF shl 16) or
                        (numbers[3].toLong(16) and 0xFFFF),
                (numbers[4].toLong(16) and 0xFFFF shl 48) or
                        (numbers[5].toLong(16) and 0xFFFF shl 32) or
                        (numbers[6].toLong(16) and 0xFFFF shl 16) or
                        (numbers[7].toLong(16) and 0xFFFF)
            )
        } catch (_: Exception) {
            throw IllegalArgumentException("IPv6 address format failed $this")
        }
    }

internal val Short.convertPortToString: String
    get() = (this.toInt() and 0xFFFF).toString()

internal val Short.convertPortToInt: Int
    get() = this.toInt() and 0xFFFF

internal val String.ipVersion: IPVersion?
    get() {
        try {
            this.convertIPv4ToInt
            return IPVersion.IPv4
        } catch (_: Exception) {
            return IPVersion.IPv6
        }
    }