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

import java.nio.ByteBuffer
import java.util.Queue

internal fun ByteBuffer.readUnsignedByte(index: Int): Int {
    return this[index].toInt() and 0x0FF
}

internal fun ByteBuffer.readUnsignedShort(index: Int): Int {
    return getShort(index).toInt() and 0x0FFFF
}

fun ByteBuffer.newString(
    position: Int = position(),
    remaining: Int = remaining()
): String {
    return String(array(), position, remaining)
}

internal fun Queue<ByteBuffer>.mergeBuffer(clear: Boolean = true): ByteBuffer {
    val pendingBuffers = this
    if (isNotEmpty()) {
        var total = 0
        for (pendingBuffer in pendingBuffers) {
            total += pendingBuffer.remaining()
        }
        var offset = 0
        val array = ByteArray(total)
        for (pendingBuffer in pendingBuffers) {
            pendingBuffer.array().copyInto(
                array,
                offset,
                pendingBuffer.position(),
                pendingBuffer.position() + pendingBuffer.remaining()
            )
            offset += pendingBuffer.remaining()
        }
        if (clear) {
            pendingBuffers.clear()
        }
        return ByteBuffer.wrap(array)
    }
    return ByteBuffer.allocate(0)
}

internal fun ByteBuffer.deepCopy(): ByteBuffer {
    return ByteBuffer.wrap(array().copyOfRange(position(), remaining()))
}