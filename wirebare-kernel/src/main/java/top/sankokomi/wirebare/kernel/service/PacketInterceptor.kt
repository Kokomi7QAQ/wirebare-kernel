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

package top.sankokomi.wirebare.kernel.service

import top.sankokomi.wirebare.kernel.net.Ipv4Header
import top.sankokomi.wirebare.kernel.net.Ipv6Header
import top.sankokomi.wirebare.kernel.net.Packet
import java.io.OutputStream

/**
 * ipv4 包拦截器，可以对 ip 包进行修改和发送
 * */
internal interface PacketInterceptor {

    /**
     * 拦截 ipv4 包
     *
     * @param ipv4Header ipv4 头
     * @param packet ip 包
     * @param outputStream 代理服务的输出流
     * */
    fun intercept(
        ipv4Header: Ipv4Header,
        packet: Packet,
        outputStream: OutputStream
    )

    /**
     * 拦截 ipv6 包
     *
     * @param ipv6Header ipv6 头
     * @param packet ip 包
     * @param outputStream 代理服务的输出流
     * */
    fun intercept(
        ipv6Header: Ipv6Header,
        packet: Packet,
        outputStream: OutputStream
    ) {}

}