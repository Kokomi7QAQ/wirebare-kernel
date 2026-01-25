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

import top.sankokomi.wirebare.kernel.util.calculateSum
import top.sankokomi.wirebare.kernel.util.readByte
import top.sankokomi.wirebare.kernel.util.readLong
import top.sankokomi.wirebare.kernel.util.readShort
import top.sankokomi.wirebare.kernel.util.writeLong
import top.sankokomi.wirebare.kernel.util.writeShort

/**
 * ipv6 包头结构如下
 *
 *    0               1               2               3
 *    0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    |Version| Traffic class |              Flow Label               |  4
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    |         Payload length        |  Next header  |   Hop limit   |  8
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    |                         Source Address                        | 12
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    |                         Source Address                        | 16
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    |                         Source Address                        | 20
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    |                         Source Address                        | 24
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    |                      Destination Address                      | 28
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    |                      Destination Address                      | 32
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    |                      Destination Address                      | 36
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    |                      Destination Address                      | 40
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    |                       Extension Headers                       | 44
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *
 */
class IPv6Header(
    val packet: ByteArray,
    val offset: Int = 0
) : IPHeader {

    companion object {
        const val IPV6_STANDARD_LENGTH = 40
        private const val OFFSET_VERSION = 0
        private const val OFFSET_PAYLOAD_LENGTH = 4
        private const val OFFSET_STANDARD_NEXT_HEADER = 6
        private const val OFFSET_SOURCE_ADDRESS_HIGH_64 = 8
        private const val OFFSET_SOURCE_ADDRESS_LOW_64 = 16
        private const val OFFSET_DESTINATION_ADDRESS_HIGH_64 = 24
        private const val OFFSET_DESTINATION_ADDRESS_LOW_64 = 32
    }

    override val ipVersion: IPVersion = IPVersion.IPv6

    override val dataProtocol: Byte

    override val headerLength: Int

    init {
        // 第一个 ipv6 拓展头从第 41 个字节开始
        var nextHeaderOffset = IPV6_STANDARD_LENGTH
        // 第一个 ipv6 拓展头的类型
        var nextHeader = standardNextHeader
        while (true) {
            val protocol = Protocol.parse(nextHeader.toByte())
            when (protocol) {
                Protocol.TCP, Protocol.UDP -> {
                    dataProtocol = nextHeader.toByte()
                    headerLength = nextHeaderOffset
                    break
                }

                Protocol.NULL -> {
                    // 拓展头部长度(8 * ExtHeaderLength) + 2 字节（NextHeader 和 ExtHeaderLength）
                    val extHeaderLength =
                        packet.readByte(offset + nextHeaderOffset + 1).toInt() and 0xFF
                    nextHeaderOffset += 8 * extHeaderLength + 2
                    nextHeader = try {
                        packet.readByte(nextHeaderOffset).toInt() and 0xFF
                    } catch (_: Exception) {
                        Protocol.END.code.toInt()
                    }
                }

                Protocol.END -> {
                    dataProtocol = Protocol.END.code
                    headerLength = 0
                    break
                }
            }
        }
    }

    override var totalLength: Int = IPV6_STANDARD_LENGTH + payloadLength

    override val dataLength: Int
        get() = totalLength - headerLength

    override var sourceAddress: IpAddress
        get() = IpAddress(
            IntIPv6(
                high64 = packet.readLong(offset + OFFSET_SOURCE_ADDRESS_HIGH_64),
                low64 = packet.readLong(offset + OFFSET_SOURCE_ADDRESS_LOW_64)
            )
        )
        set(value) {
            packet.writeLong(value.intIPv6.high64, offset + OFFSET_SOURCE_ADDRESS_HIGH_64)
            packet.writeLong(value.intIPv6.low64, offset + OFFSET_SOURCE_ADDRESS_LOW_64)
        }

    override var destinationAddress: IpAddress
        get() = IpAddress(
            IntIPv6(
                high64 = packet.readLong(offset + OFFSET_DESTINATION_ADDRESS_HIGH_64),
                low64 = packet.readLong(offset + OFFSET_DESTINATION_ADDRESS_LOW_64)
            )
        )
        set(value) {
            packet.writeLong(value.intIPv6.high64, offset + OFFSET_DESTINATION_ADDRESS_HIGH_64)
            packet.writeLong(value.intIPv6.low64, offset + OFFSET_DESTINATION_ADDRESS_LOW_64)
        }

    override val addressSum: Int
        get() = packet.calculateSum(offset + OFFSET_SOURCE_ADDRESS_HIGH_64, 32)

    override fun notifyCheckSum() {
        // ipv6 没有校验和
    }

    val version: Int
        get() = packet.readByte(offset + OFFSET_VERSION).toInt() ushr 4

    var payloadLength: Int
        get() = packet.readShort(offset + OFFSET_PAYLOAD_LENGTH).toInt() and 0xFFFF
        set(value) = packet.writeShort(value.toShort(), offset + OFFSET_STANDARD_NEXT_HEADER)

    val standardNextHeader: Int
        get() = packet.readByte(OFFSET_STANDARD_NEXT_HEADER).toInt() and 0xFF

}