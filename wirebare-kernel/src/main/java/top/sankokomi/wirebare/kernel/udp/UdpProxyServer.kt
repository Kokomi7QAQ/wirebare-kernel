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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import top.sankokomi.wirebare.kernel.common.WireBareConfiguration
import top.sankokomi.wirebare.kernel.net.Port
import top.sankokomi.wirebare.kernel.net.UdpHeader
import top.sankokomi.wirebare.kernel.net.UdpSessionStore
import top.sankokomi.wirebare.kernel.proxy.NioProxyServer
import top.sankokomi.wirebare.kernel.service.WireBareProxyService
import top.sankokomi.wirebare.kernel.util.WireBareLogger
import top.sankokomi.wirebare.kernel.util.closeSafely
import top.sankokomi.wirebare.kernel.util.convertPortToInt
import java.io.OutputStream
import java.nio.channels.DatagramChannel
import java.nio.channels.Selector

/**
 * UDP 代理服务器
 *
 * 负责启动 [UdpRealTunnel] 并让 [UdpRealTunnel] 与远程服务器通信
 * */
internal class UdpProxyServer(
    private val sessionStore: UdpSessionStore,
    private val configuration: WireBareConfiguration,
    private val proxyService: WireBareProxyService
) : NioProxyServer(), CoroutineScope by proxyService {

    override val selector: Selector = Selector.open()

    private val tunnels = hashMapOf<Port, UdpRealTunnel>()

    /**
     * 开始代理 UDP 数据包
     * */
    internal fun proxy(
        udpHeader: UdpHeader,
        outputStream: OutputStream
    ) {
        launch(Dispatchers.IO) {
            val sourcePort = udpHeader.sourcePort
            try {
                val tunnel =
                    tunnels[sourcePort] ?: createTunnel(udpHeader, outputStream)
                tunnel.write(udpHeader.data)
            } catch (e: Exception) {
                tunnels.remove(sourcePort)?.closeSafely()
                WireBareLogger.error(e)
            }
        }
    }

    /**
     * 创建一个 [UdpRealTunnel]
     * */
    private fun createTunnel(
        udpHeader: UdpHeader,
        outputStream: OutputStream
    ): UdpRealTunnel {
        WireBareLogger.debug("[UDP] 通道创建 客户端 ${udpHeader.sourcePort} >> 代理服务器")
        val session = sessionStore.query(
            udpHeader.sourcePort
        ) ?: throw IllegalStateException("一个 UDP 请求因为找不到指定会话而代理失败")

        return UdpRealTunnel(
            DatagramChannel.open(),
            selector,
            outputStream,
            session,
            udpHeader,
            configuration,
            proxyService
        ).also {
            it.connectRemoteServer(
                udpHeader.ipHeader.destinationAddress.stringIp,
                udpHeader.destinationPort.port.convertPortToInt
            )
            tunnels[session.sourcePort] = it
        }
    }

    override fun release() {
        for (tunnel in tunnels.values) {
            tunnel.closeSafely()
        }
        tunnels.clear()
    }

}