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

package top.sankokomi.wirebare.kernel.interceptor.ssl

import top.sankokomi.wirebare.kernel.interceptor.http.HttpInterceptChain
import top.sankokomi.wirebare.kernel.interceptor.http.HttpInterceptor
import top.sankokomi.wirebare.kernel.interceptor.http.HttpSession
import top.sankokomi.wirebare.kernel.interceptor.tcp.TcpTunnel
import top.sankokomi.wirebare.kernel.net.TcpSession
import top.sankokomi.wirebare.kernel.ssl.JKS
import top.sankokomi.wirebare.kernel.ssl.RequestSSLCodec
import top.sankokomi.wirebare.kernel.ssl.ResponseSSLCodec
import top.sankokomi.wirebare.kernel.ssl.SSLCallback
import top.sankokomi.wirebare.kernel.ssl.SSLEngineFactory
import top.sankokomi.wirebare.kernel.util.WireBareLogger
import top.sankokomi.wirebare.kernel.util.mergeBuffer
import java.nio.ByteBuffer
import java.util.concurrent.LinkedBlockingQueue

class HttpSSLCodecInterceptor(jks: JKS) : HttpInterceptor {

    companion object {
        private const val TAG = "HttpSSLCodecInterceptor"
    }

    private val factory = SSLEngineFactory(jks)

    internal val requestCodec = RequestSSLCodec(factory)

    internal val responseCodec = ResponseSSLCodec(factory)

    private val pendingReqCiphertextMap =
        hashMapOf<TcpSession, LinkedBlockingQueue<ByteBuffer>>()

    private val pendingRspCiphertextMap =
        hashMapOf<TcpSession, LinkedBlockingQueue<ByteBuffer>>()

    override fun onRequest(
        chain: HttpInterceptChain,
        buffer: ByteBuffer,
        session: HttpSession,
        tunnel: TcpTunnel
    ) {
        val (request, _, tcpSession) = session
        if (request.isHttps != true) {
            super.onRequest(chain, buffer, session, tunnel)
            return
        }
        val host = request.hostInternal ?: return
        responseCodec.handshakeIfNecessary(
            tcpSession,
            host,
            object : SSLCallback {
                override fun encryptSuccess(target: ByteBuffer) {
                    tunnel.writeToRemoteServer(target)
                }
            }
        )
        val pendingReqCiphertext = pendingReqCiphertext(tcpSession)
        pendingReqCiphertext.add(buffer)
        requestCodec.decode(
            tcpSession,
            host,
            pendingReqCiphertext.mergeBuffer(),
            object : SSLCallback {
                override fun shouldPending(target: ByteBuffer) {
                    pendingReqCiphertext.add(target)
                }

                override fun sslFailed(target: ByteBuffer) {
                    WireBareLogger.warn(TAG, "request SSL engine create failed")
                }

                override fun decryptSuccess(target: ByteBuffer) {
                    session.request.isPlaintext = true
                    chain.processRequestNext(
                        this@HttpSSLCodecInterceptor,
                        target,
                        session,
                        tunnel
                    )
                }

                override fun encryptSuccess(target: ByteBuffer) {
                    tunnel.writeToLocalClient(target)
                }
            }
        )
    }

    override fun onResponse(
        chain: HttpInterceptChain,
        buffer: ByteBuffer,
        session: HttpSession,
        tunnel: TcpTunnel
    ) {
        val (_, response, tcpSession) = session
        if (response.isHttps != true) {
            super.onResponse(chain, buffer, session, tunnel)
            return
        }
        val host = response.hostInternal ?: return
        val pendingRspCiphertext = pendingRspCiphertext(tcpSession)
        pendingRspCiphertext.add(buffer)
        responseCodec.decode(
            tcpSession,
            host,
            pendingRspCiphertext.mergeBuffer(),
            object : SSLCallback {
                override fun shouldPending(target: ByteBuffer) {
                    pendingRspCiphertext.add(target)
                }

                override fun sslFailed(target: ByteBuffer) {
                    WireBareLogger.warn(TAG, "response SSL engine create failed")
                    // chain.processRequestNext(buffer, session, tunnel)
                }

                override fun decryptSuccess(target: ByteBuffer) {
                    session.response.isPlaintext = true
                    chain.processResponseNext(
                        this@HttpSSLCodecInterceptor,
                        target,
                        session,
                        tunnel
                    )
                }

                override fun encryptSuccess(target: ByteBuffer) {
                    tunnel.writeToRemoteServer(target)
                }
            }
        )
    }

    override fun onRequestFinished(
        chain: HttpInterceptChain,
        session: HttpSession,
        tunnel: TcpTunnel
    ) {
        super.onRequestFinished(chain, session, tunnel)
        pendingReqCiphertextMap.remove(session.tcpSession)
    }

    override fun onResponseFinished(
        chain: HttpInterceptChain,
        session: HttpSession,
        tunnel: TcpTunnel
    ) {
        super.onResponseFinished(chain, session, tunnel)
        pendingRspCiphertextMap.remove(session.tcpSession)
    }

    private fun pendingReqCiphertext(
        tcpSession: TcpSession
    ): LinkedBlockingQueue<ByteBuffer> {
        return pendingReqCiphertextMap.getOrPut(tcpSession) {
            LinkedBlockingQueue()
        }
    }

    private fun pendingRspCiphertext(
        tcpSession: TcpSession
    ): LinkedBlockingQueue<ByteBuffer> {
        return pendingRspCiphertextMap.getOrPut(tcpSession) {
            LinkedBlockingQueue()
        }
    }
}