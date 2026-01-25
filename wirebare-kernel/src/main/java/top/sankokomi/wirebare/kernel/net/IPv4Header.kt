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
import top.sankokomi.wirebare.kernel.util.readInt
import top.sankokomi.wirebare.kernel.util.readShort
import top.sankokomi.wirebare.kernel.util.writeByte
import top.sankokomi.wirebare.kernel.util.writeInt
import top.sankokomi.wirebare.kernel.util.writeShort
import java.math.BigInteger
import kotlin.experimental.and

/**
 * ipv4 包头结构如下
 *
 *    0               1               2               3
 *    0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    |Version|  IHL  |Type of Service|          Total Length         |  4
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    |         Identification        |Flags|      Fragment Offset    |  8
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    |  Time to Live |    Protocol   |         Header Checksum       | 12
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    |                         Source Address                        | 16
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    |                      Destination Address                      | 20
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    |                    Options                    |    Padding    | 24
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *
 * IHL := IP Header Length
 */
class IPv4Header(
    private val packet: ByteArray,
    private val offset: Int = 0
) : IPHeader {

    companion object {
        const val MIN_IPV4_LENGTH = 20
        private const val OFFSET_VERSION = 0
        private const val OFFSET_IP_HEADER_LENGTH = 0
        private const val OFFSET_TOTAL_LENGTH = 2
        private const val OFFSET_IDENTIFICATION = 4
        private const val OFFSET_FLAGS = 6
        private const val OFFSET_FRAGMENT_OFFSET = 6
        private const val OFFSET_PROTOCOL = 9
        private const val OFFSET_CHECK_SUM = 10
        private const val OFFSET_SOURCE_ADDRESS = 12
        private const val OFFSET_DESTINATION_ADDRESS = 16
        private const val MASK_MF = 0b00100000.toByte()
        private const val MASK_DF = 0b01000000.toByte()
    }

    override val ipVersion: IPVersion = IPVersion.IPv4

    override var dataProtocol: Byte
        get() = packet[offset + OFFSET_PROTOCOL]
        set(value) = packet.writeByte(value, offset + OFFSET_PROTOCOL)

    override val headerLength: Int
        get() = packet.readByte(offset + OFFSET_IP_HEADER_LENGTH).toInt() and 0xF shl 2

    override val dataLength: Int
        get() = totalLength - headerLength

    override var totalLength: Int
        get() = packet.readShort(offset + OFFSET_TOTAL_LENGTH).toInt() and 0xFFFF
        set(value) = packet.writeShort(value.toShort(), offset + OFFSET_TOTAL_LENGTH)

    override var sourceAddress: IpAddress
        get() = IpAddress(packet.readInt(offset + OFFSET_SOURCE_ADDRESS))
        set(value) = packet.writeInt(value.intIPv4, offset + OFFSET_SOURCE_ADDRESS)

    override var destinationAddress: IpAddress
        get() = IpAddress(packet.readInt(offset + OFFSET_DESTINATION_ADDRESS))
        set(value) = packet.writeInt(value.intIPv4, offset + OFFSET_DESTINATION_ADDRESS)

    /**
     * 计算来源 ip 地址和目的 ip 地址的异或和并返回
     * */
    override val addressSum: Int
        get() = packet.calculateSum(offset + OFFSET_SOURCE_ADDRESS, 8)

    /**
     * 先将 ip 头中的校验和置为 0 ，然后重新计算校验和
     * */
    override fun notifyCheckSum() {
        checkSum = 0.toShort()
        checkSum = calculateChecksum()
    }

    val version: Int
        get() = packet.readByte(offset + OFFSET_VERSION).toInt() ushr 4

    val identification: Short get() = packet.readShort(OFFSET_IDENTIFICATION)

    val flags: Byte get() = packet.readByte(OFFSET_FLAGS)

    val mf: Boolean get() = flags and MASK_MF == MASK_MF

    val df: Boolean get() = flags and MASK_DF == MASK_DF

    val fragmentOffset: Short get() = packet.readShort(OFFSET_FRAGMENT_OFFSET) and 0x1FFF

    var checkSum: Short
        get() = packet.readShort(offset + OFFSET_CHECK_SUM)
        private set(value) = packet.writeShort(value, offset + OFFSET_CHECK_SUM)

    private fun calculateChecksum(): Short {
        var sum = packet.calculateSum(offset, headerLength)
        while ((sum shr 16) != 0) {
            sum = (sum and 0xFFFF) + (sum ushr 16)
        }
        return sum.inv().toShort()
    }

}