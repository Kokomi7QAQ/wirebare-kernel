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

package top.sankokomi.wirebare.kernel.interceptor.http

import top.sankokomi.wirebare.kernel.interceptor.tcp.TcpTunnel
import top.sankokomi.wirebare.kernel.ssl.RequestSSLCodec
import top.sankokomi.wirebare.kernel.ssl.ResponseSSLCodec
import top.sankokomi.wirebare.kernel.ssl.SSLCallback
import top.sankokomi.wirebare.kernel.util.WireBareLogger
import java.nio.ByteBuffer

class HttpFlushInterceptor(
    private val requestCodec: RequestSSLCodec? = null,
    private val responseCodec: ResponseSSLCodec? = null
) : HttpInterceptor {

    override fun onRequest(
        chain: HttpInterceptChain,
        buffer: ByteBuffer,
        session: HttpSession,
        tunnel: TcpTunnel
    ) {
        if (session.request.isHttps == true && session.request.isPlaintext == true) {
            val (request, _, tcpSession) = session
            val host = request.hostInternal ?: return
            responseCodec?.encode(
                tcpSession,
                host,
                buffer,
                object : SSLCallback {
                    override fun encryptSuccess(target: ByteBuffer) {
                        tunnel.writeToRemoteServer(target)
                    }
                }
            ) ?: WireBareLogger.warn("HTTPS 请求报文被解密了但却没有编码器")
        } else {
            tunnel.writeToRemoteServer(buffer)
        }
        super.onRequest(chain, buffer, session, tunnel)
    }

    override fun onResponse(
        chain: HttpInterceptChain,
        buffer: ByteBuffer,
        session: HttpSession,
        tunnel: TcpTunnel
    ) {
        if (session.response.isHttps == true && session.response.isPlaintext == true) {
            val (_, response, tcpSession) = session
            val host = response.hostInternal ?: return
            requestCodec?.encode(
                tcpSession,
                host,
                buffer,
                object : SSLCallback {
                    override fun encryptSuccess(target: ByteBuffer) {
                        tunnel.writeToLocalClient(target)
                    }
                }
            ) ?: WireBareLogger.warn("HTTPS 响应报文被解密了但却没有编码器")
        } else {
            tunnel.writeToLocalClient(buffer)
        }
        super.onRequest(chain, buffer, session, tunnel)
    }

}