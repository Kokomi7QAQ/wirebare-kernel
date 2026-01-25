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

import top.sankokomi.wirebare.kernel.common.WireBareConfiguration
import top.sankokomi.wirebare.kernel.interceptor.tcp.TcpTunnel
import top.sankokomi.wirebare.kernel.interceptor.tcp.TcpVirtualGateway
import top.sankokomi.wirebare.kernel.net.Port
import top.sankokomi.wirebare.kernel.net.TcpSession
import top.sankokomi.wirebare.kernel.nio.SocketNioTunnel
import top.sankokomi.wirebare.kernel.util.WireBareLogger
import top.sankokomi.wirebare.kernel.util.closeSafely
import java.nio.ByteBuffer
import java.nio.channels.Selector
import java.nio.channels.SocketChannel

/**
 * [TcpProxyTunnel] 会将被 [TcpProxyServer] 拦截的请求字节流转发给 [TcpRealTunnel] ，
 * [TcpRealTunnel] 请求远程服务器后会将远程服务器响应的字节流转发给 [TcpProxyTunnel] ，
 * 最后 [TcpProxyTunnel] 会将接收到的来自 [TcpRealTunnel] 的响应字节流返回给 [TcpProxyServer] ，
 * [TcpProxyServer] 将响应字节流返回给被代理的客户端，这样就完成了整个代理过程
 *
 * 在 [TcpProxyTunnel] 将请求字节流转发给 [TcpRealTunnel] 之前，
 * 会先将代理请求的字节流交由 [tcpVirtualGateway] 来进行拦截，然后再进行转发
 *
 * 在 [TcpProxyTunnel] 接收到由 [TcpRealTunnel] 转发的响应字节流之后，
 * 会先将代理响应的字节流交由 [tcpVirtualGateway] 来进行拦截，然后再转发给 [TcpProxyServer]
 *
 * 请求过程如下（包含拦截）：
 *
 * Real Client >> [TcpProxyServer] >> [TcpProxyTunnel] >>
 * [tcpVirtualGateway] >> [TcpRealTunnel] >> Remote Server
 *
 * 响应过程如下（包含拦截）：
 *
 * Remote Server >> [TcpRealTunnel] >> [TcpProxyTunnel] >>
 * [tcpVirtualGateway] >> [TcpProxyServer] >> Real Client
 *
 * @see TcpProxyServer
 * @see TcpRealTunnel
 * */
internal class TcpProxyTunnel(
    override val channel: SocketChannel,
    override val selector: Selector,
    internal val port: Port,
    private val session: TcpSession,
    private val tcpVirtualGateway: TcpVirtualGateway,
    private val configuration: WireBareConfiguration
) : SocketNioTunnel(), TcpTunnel {

    companion object {
        private const val TAG = "TcpProxyTunnel"
    }

    private lateinit var realTunnel: TcpRealTunnel

    internal fun attachRealChannel(real: TcpRealTunnel) {
        realTunnel = real
    }

    override fun onConnected() {
        prepareRead()
    }

    override fun onWrite(): Int {
        val length = super.onWrite()
        WireBareLogger.inetVerbose(
            TAG,
            session,
            "proxy client $port < proxy server $length bytes"
        )
        return length
    }

    override fun onRead() {
        if (isClosed) {
            tcpVirtualGateway.onResponseFinished(session, this)
            return
        }
        val buffer = ByteBuffer.allocate(configuration.mtu)
        val length = read(buffer)
        if (length < 0 || realTunnel.isClosed) {
            closeSafely()
            tcpVirtualGateway.onResponseFinished(session, this)
            return
        }
        WireBareLogger.inetVerbose(
            TAG,
            session,
            "proxy client $port > proxy server $length bytes"
        )
        tcpVirtualGateway.onRequest(buffer, session, this)
    }

    override fun onException(t: Throwable) {
        closeSafely(this, realTunnel)
        tcpVirtualGateway.onResponseFinished(session, this)
    }

    override fun writeToRemoteServer(buffer: ByteBuffer) {
        realTunnel.write(buffer)
    }

    override fun writeToLocalClient(buffer: ByteBuffer) {
        write(buffer)
    }
}