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

package top.sankokomi.wirebare.kernel.udp

import top.sankokomi.wirebare.kernel.common.WireBareConfiguration
import top.sankokomi.wirebare.kernel.net.IPHeader
import top.sankokomi.wirebare.kernel.net.Packet
import top.sankokomi.wirebare.kernel.net.UdpHeader
import top.sankokomi.wirebare.kernel.net.UdpSessionStore
import top.sankokomi.wirebare.kernel.service.PacketInterceptor
import top.sankokomi.wirebare.kernel.service.WireBareProxyService
import top.sankokomi.wirebare.kernel.util.WireBareLogger
import java.io.OutputStream

/**
 * UDP 报文拦截器
 *
 * 拦截被代理客户端的数据包并转发给 [UdpProxyServer]
 * */
internal class UdpPacketInterceptor(
    configuration: WireBareConfiguration,
    proxyService: WireBareProxyService
) : PacketInterceptor {

    companion object {
        private const val TAG = "UdpPacketInterceptor"
    }

    private val sessionStore: UdpSessionStore = UdpSessionStore()

    private val proxyServer =
        UdpProxyServer(sessionStore, configuration, proxyService).apply { dispatch() }

    override fun intercept(
        ipHeader: IPHeader,
        packet: Packet,
        outputStream: OutputStream
    ) {
        val udpHeader = UdpHeader(ipHeader, packet.packet, ipHeader.headerLength)

        val sourceAddress = ipHeader.sourceAddress
        val sourcePort = udpHeader.sourcePort

        val destinationAddress = ipHeader.destinationAddress
        val destinationPort = udpHeader.destinationPort

        val session = sessionStore.insert(
            sourceAddress,
            sourcePort,
            destinationAddress,
            destinationPort
        )

        WireBareLogger.inetDebug(TAG, session, "start")

        proxyServer.proxy(udpHeader, outputStream)
    }
}