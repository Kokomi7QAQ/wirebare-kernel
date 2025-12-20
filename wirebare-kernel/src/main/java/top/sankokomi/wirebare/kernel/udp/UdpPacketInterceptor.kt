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
import top.sankokomi.wirebare.kernel.net.Ipv4Header
import top.sankokomi.wirebare.kernel.net.Ipv6Header
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

    private val sessionStore: UdpSessionStore = UdpSessionStore()

    private val proxyServer =
        UdpProxyServer(sessionStore, configuration, proxyService).apply { dispatch() }

    override fun intercept(
        ipv4Header: Ipv4Header,
        packet: Packet,
        outputStream: OutputStream
    ) {
        val udpHeader = UdpHeader(ipv4Header, packet.packet, ipv4Header.headerLength)

        val sourcePort = udpHeader.sourcePort

        val destinationAddress = ipv4Header.destinationAddress
        val destinationPort = udpHeader.destinationPort

        val session = sessionStore.insert(
            sourcePort,
            destinationAddress,
            destinationPort
        )

        WireBareLogger.inetDebug(session, "[IPv4-UDP] 客户端 $sourcePort >> 代理服务器")

        proxyServer.proxy(udpHeader, outputStream)
    }

    override fun intercept(
        ipv6Header: Ipv6Header,
        packet: Packet,
        outputStream: OutputStream
    ) {
        val udpHeader = UdpHeader(ipv6Header, packet.packet, ipv6Header.headerLength)

        val sourcePort = udpHeader.sourcePort

        val destinationAddress = ipv6Header.destinationAddress
        val destinationPort = udpHeader.destinationPort

        val session = sessionStore.insert(
            sourcePort,
            destinationAddress,
            destinationPort
        )

        WireBareLogger.inetDebug(session, "[IPv6-UDP] 客户端 $sourcePort >> 代理服务器")

        proxyServer.proxy(udpHeader, outputStream)
    }
}