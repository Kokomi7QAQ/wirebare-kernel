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

import top.sankokomi.wirebare.kernel.util.readByte
import java.lang.Exception

internal fun resolveTargetNextHeaderOffset(
    packet: ByteArray,
    offset: Int,
    standardNextHeader: Int
): Pair<Int, Int> {
    var nextHeaderOffset = Ipv6Header.IPV6_STANDARD_LENGTH
    var nextHeader = standardNextHeader
    while (true) {
        if (Protocol.parse(nextHeader.toByte()) == Protocol.NULL) {
            val length = (resolverMap[nextHeader] ?: NormalExtHeaderLengthResolver).resolveLength(
                packet,
                offset + nextHeaderOffset
            )
            nextHeaderOffset += length
            nextHeader = try {
                packet.readByte(nextHeaderOffset).toInt() and 0xFF
            } catch (_: Exception) {
                return 59 to 0
            }
        } else {
            return nextHeader to nextHeaderOffset
        }
    }
}

private val resolverSet = hashSetOf<Ipv6ExtHeaderLengthResolver>()

private val resolverMap = hashMapOf<Int, Ipv6ExtHeaderLengthResolver>().also {
    for (resolver in resolverSet) {
        it[resolver.nextHeaderToResolve()] = resolver
    }
}

object NormalExtHeaderLengthResolver : Ipv6ExtHeaderLengthResolver {
    override fun nextHeaderToResolve(): Int {
        throw NotImplementedError()
    }

    override fun resolveLength(packet: ByteArray, offset: Int): Int {
        // 拓展头部长度(8 * ExtHeaderLength) + 2 字节（NextHeader 和 ExtHeaderLength）
        return (packet.readByte(offset + 1).toInt() and 0xFF) * 8 + 2
    }
}

//object HopByHopOptionsHeaderLengthResolver : NormalExtHeaderLengthResolver() {
//    override fun nextHeaderToResolve(): Int = 0
//}
//
//object DestinationOptionsHeaderLengthResolver : NormalExtHeaderLengthResolver() {
//    override fun nextHeaderToResolve(): Int = 60
//}
//
//object RoutingHeaderLengthResolver : NormalExtHeaderLengthResolver() {
//    override fun nextHeaderToResolve(): Int = 43
//}
//
//object FragmentHeaderLengthResolver : NormalExtHeaderLengthResolver() {
//    override fun nextHeaderToResolve(): Int = 44
//}