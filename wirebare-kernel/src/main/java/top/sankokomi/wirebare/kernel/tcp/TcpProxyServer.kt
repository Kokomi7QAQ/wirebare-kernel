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

import kotlinx.coroutines.CoroutineScope
import top.sankokomi.wirebare.kernel.common.WireBareConfiguration
import top.sankokomi.wirebare.kernel.interceptor.tcp.TcpVirtualGateway
import top.sankokomi.wirebare.kernel.net.Port
import top.sankokomi.wirebare.kernel.net.TcpSessionStore
import top.sankokomi.wirebare.kernel.nio.NioCallback
import top.sankokomi.wirebare.kernel.proxy.NioProxyServer
import top.sankokomi.wirebare.kernel.service.WireBareProxyService
import top.sankokomi.wirebare.kernel.util.WireBareLogger
import java.net.InetSocketAddress
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel

/**
 * TCP 代理服务器
 *
 * 负责接收被代理客户端的请求，并通过 [TcpProxyTunnel]
 * 和 [TcpRealTunnel] 来与远程服务器进行通信并进行拦截处理，之后将请求结果返回给被代理客户端
 *
 * 请求过程如下（不包含拦截）：
 *
 * Real Client >> [TcpProxyServer] >>
 * [TcpProxyTunnel] >> [TcpRealTunnel] >>
 * Remote Server
 *
 * 响应过程如下（不包含拦截）：
 *
 * Remote Server >> [TcpRealTunnel] >>
 * [TcpProxyTunnel] >> [TcpProxyServer] >>
 * Real Server
 *
 * @see [TcpProxyTunnel]
 * @see [TcpRealTunnel]
 * */
internal class TcpProxyServer(
    private val sessionStore: TcpSessionStore,
    private val tcpVirtualGateway: TcpVirtualGateway,
    private val configuration: WireBareConfiguration,
    private val proxyService: WireBareProxyService
) : NioProxyServer(), NioCallback, CoroutineScope by proxyService {

    companion object {
        private const val TAG = "TcpProxyServer"
    }

    internal val proxyServerPort: Port

    override val selector: Selector = Selector.open()

    private val proxyServerSocketChannel = ServerSocketChannel.open().apply {
        configureBlocking(false)
        socket().bind(InetSocketAddress(0))
        register(selector, SelectionKey.OP_ACCEPT, this@TcpProxyServer)
        proxyServerPort = Port(socket().localPort.toShort())
    }

    override fun onAccept() {
        val proxySocketChannel = proxyServerSocketChannel.accept()
        val proxySocket = proxySocketChannel.socket()

        // 这个端口号就是这次请求的来源端口号
        val sourcePort = Port(proxySocket.port.toShort())
        val session = sessionStore.query(
            sourcePort
        ) ?: throw IllegalStateException("cannot find TCP session(port = $sourcePort")

        WireBareLogger.inetInfo(TAG, session, "create tunnel")

        // 接收到被代理客户端的请求后开始代理
        val proxyTunnel = TcpProxyTunnel(
            proxySocketChannel,
            selector,
            proxyServerPort,
            session,
            tcpVirtualGateway,
            configuration
        )
        val realTunnel = TcpRealTunnel(
            SocketChannel.open(),
            selector,
            session,
            configuration,
            tcpVirtualGateway,
            proxyService
        )

        // 将 TcpProxyTunnel 与 TcpReadTunnel 关联并开始连接远程服务器
        proxyTunnel.attachRealChannel(realTunnel)
        realTunnel.attachProxyTunnel(proxyTunnel)
        realTunnel.connectRemoteServer()
    }

    override fun onConnected() {
    }

    override fun onRead() {
    }

    override fun onWrite(): Int {
        return -1
    }

    override fun onException(t: Throwable) {
    }

    override fun release() {
        proxyServerSocketChannel.close()
    }

}