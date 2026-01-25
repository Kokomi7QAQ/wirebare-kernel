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

package top.sankokomi.wirebare.kernel.net

import top.sankokomi.wirebare.kernel.util.WireBareLogger
import top.sankokomi.wirebare.kernel.util.readByte
import java.math.BigInteger

interface IPHeader {

    companion object {
        private const val TAG = "IPHeader"
        private const val VERSION_4 = 0b0100
        private const val VERSION_6 = 0b0110

        internal fun parse(packet: ByteArray, length: Int, offset: Int): IPHeader? {
            when (val ipVersion = packet.readByte(offset).toInt() ushr 4) {
                VERSION_4 -> {
                    if (length < IPv4Header.MIN_IPV4_LENGTH) {
                        WireBareLogger.warn(TAG, "IPv4 packet length($length) less than min(${IPv4Header.MIN_IPV4_LENGTH})")
                        return null
                    }
                    return IPv4Header(packet, 0)
                }

                VERSION_6 -> {
                    if (length < IPv6Header.IPV6_STANDARD_LENGTH) {
                        WireBareLogger.warn(TAG, "IPv6 packet length($length) less than min(${IPv6Header.IPV6_STANDARD_LENGTH})")
                        return null
                    }
                    return IPv6Header(packet, 0)
                }

                else -> {
                    WireBareLogger.warn(TAG, "unknow IP version 0b${ipVersion.toString(2)}")
                    return null
                }
            }
        }
    }

    val ipVersion: IPVersion

    /**
     * 协议版本号
     * */
    val dataProtocol: Byte

    /**
     * 整个 IP 头的长度
     * */
    val headerLength: Int

    /**
     * 去掉 IP 头后数据段长度
     * */
    val dataLength: Int

    /**
     * 整个 IP 包的长度
     * */
    var totalLength: Int

    var sourceAddress: IpAddress

    var destinationAddress: IpAddress

    /**
     * 地址部分的校验和
     * */
    val addressSum: BigInteger

    fun notifyCheckSum()
}