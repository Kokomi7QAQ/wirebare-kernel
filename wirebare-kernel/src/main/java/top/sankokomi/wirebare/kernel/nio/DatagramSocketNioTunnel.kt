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

package top.sankokomi.wirebare.kernel.nio

import java.io.Closeable
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel

/**
 * udp 套接字的 [NioTunnel] 实现
 * */
internal abstract class DatagramSocketNioTunnel : NioTunnel<DatagramChannel>(), Closeable {

    /**
     * udp 套接字通道
     * */
    abstract override val channel: DatagramChannel

    final override fun onConnected() {
        throw IllegalStateException("UDP 不是面向连接的通信，不要激活可连接操作")
    }

    override fun readByteBuffer(buffer: ByteBuffer): Int = channel.read(buffer)

    override fun writeByteBuffer(buffer: ByteBuffer): Int = channel.write(buffer)

}