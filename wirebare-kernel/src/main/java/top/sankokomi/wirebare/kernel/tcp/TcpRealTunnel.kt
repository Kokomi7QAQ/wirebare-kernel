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

package top.sankokomi.wirebare.kernel.tcp

import top.sankokomi.wirebare.kernel.common.EventSynopsis
import top.sankokomi.wirebare.kernel.common.ImportantEvent
import top.sankokomi.wirebare.kernel.common.WireBare
import top.sankokomi.wirebare.kernel.common.WireBareConfiguration
import top.sankokomi.wirebare.kernel.interceptor.tcp.TcpTunnel
import top.sankokomi.wirebare.kernel.interceptor.tcp.TcpVirtualGateway
import top.sankokomi.wirebare.kernel.net.IPVersion
import top.sankokomi.wirebare.kernel.net.TcpSession
import top.sankokomi.wirebare.kernel.nio.SocketNioTunnel
import top.sankokomi.wirebare.kernel.service.WireBareProxyService
import top.sankokomi.wirebare.kernel.util.WireBareLogger
import top.sankokomi.wirebare.kernel.util.closeSafely
import top.sankokomi.wirebare.kernel.util.ipVersion
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.SocketChannel

/**
 * [TcpRealTunnel] 会接收来自 [TcpProxyTunnel] 的请求字节流，
 * 将请求字节流发送到远程服务器中，接收远程服务器的响应字节流，
 * 并将响应字节流转发给 [TcpProxyTunnel] 进行处理
 *
 * @see TcpProxyServer
 * */
internal class TcpRealTunnel(
    override val channel: SocketChannel,
    override val selector: Selector,
    private val session: TcpSession,
    private val configuration: WireBareConfiguration,
    private val tcpVirtualGateway: TcpVirtualGateway,
    private val proxyService: WireBareProxyService
) : SocketNioTunnel(), TcpTunnel {

    companion object {
        private const val TAG = "TcpRealTunnel"
    }

    private lateinit var proxyTunnel: TcpProxyTunnel

    internal fun attachProxyTunnel(proxy: TcpProxyTunnel) {
        proxyTunnel = proxy
    }

    private val remoteAddress = session.destinationAddress.stringIP
    private val remotePort = session.destinationPort.port.toInt() and 0xFFFF

    internal fun connectRemoteServer() {
        if (proxyService.protect(channel.socket())) {
            channel.configureBlocking(false)
            channel.register(selector, SelectionKey.OP_CONNECT, this)
            WireBareLogger.warn(TAG, "start to connect remote server($remoteAddress:$remotePort)")
            try {
                channel.connect(InetSocketAddress(remoteAddress, remotePort))
            } catch (e: Exception) {
                reportExceptionWhenConnect(remoteAddress, remotePort, e)
                WireBareLogger.error(
                    TAG,
                    "connect to remote server($remoteAddress:$remotePort) failed",
                    e
                )
                onException(e)
            }
        } else {
            throw IllegalStateException("cannot protect socket for tcp tunnel")
        }
    }

    override fun onConnected() {
        WireBareLogger.warn(TAG, "connect remote server succeed $remoteAddress:$remotePort")
        if (channel.finishConnect()) {
            proxyTunnel.onConnected()
            prepareRead()
        } else {
            throw IllegalStateException("connect remote server failed")
        }
    }

    override fun onWrite(): Int {
        val length = super.onWrite()
        WireBareLogger.inetVerbose(
            TAG,
            session,
            "proxy server > remote server $length bytes"
        )
        return length
    }

    override fun onRead() {
        if (isClosed) {
            tcpVirtualGateway.onRequestFinished(session, this)
            return
        }
        val buffer = ByteBuffer.allocate(configuration.mtu)
        val length = read(buffer)
        if (length < 0 || proxyTunnel.isClosed) {
            closeSafely()
            tcpVirtualGateway.onRequestFinished(session, this)
            return
        }
        WireBareLogger.inetVerbose(
            TAG,
            session,
            "proxy server < remote server $length Bytes"
        )
        tcpVirtualGateway.onResponse(buffer, session, this)
    }

    private fun reportExceptionWhenConnect(address: String, port: Int, t: Throwable?) {
        when (address.ipVersion) {
            IPVersion.IPv4 -> {
                WireBare.postImportantEvent(
                    ImportantEvent(
                        "[TCP] try to connect to $address:$port failed",
                        EventSynopsis.IPV4_UNREACHABLE,
                        t
                    )
                )
            }

            IPVersion.IPv6 -> {
                WireBare.postImportantEvent(
                    ImportantEvent(
                        "[TCP] try to connect to $address:$port failed",
                        EventSynopsis.IPV6_UNREACHABLE,
                        t
                    )
                )
            }

            else -> {}
        }
    }

    override fun onException(t: Throwable) {
        closeSafely(this, proxyTunnel)
        tcpVirtualGateway.onRequestFinished(session, this)
    }

    override fun writeToRemoteServer(buffer: ByteBuffer) {
        write(buffer)
    }

    override fun writeToLocalClient(buffer: ByteBuffer) {
        proxyTunnel.write(buffer)
    }
}