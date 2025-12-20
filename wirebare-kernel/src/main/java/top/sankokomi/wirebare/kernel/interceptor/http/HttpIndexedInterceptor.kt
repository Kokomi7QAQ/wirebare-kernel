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
import top.sankokomi.wirebare.kernel.util.UnsupportedCall
import java.nio.ByteBuffer

abstract class HttpIndexedInterceptor : HttpInterceptor {

    private val reqIndexMap = hashMapOf<HttpSession, Int>()
    private val rspIndexMap = hashMapOf<HttpSession, Int>()

    open fun onRequest(
        chain: HttpInterceptChain,
        buffer: ByteBuffer,
        session: HttpSession,
        tunnel: TcpTunnel,
        index: Int
    ) {
        chain.processRequestNext(this, buffer, session, tunnel)
    }

    open fun onRequestFinished(
        chain: HttpInterceptChain,
        session: HttpSession,
        tunnel: TcpTunnel,
        index: Int
    ) {
        chain.processRequestFinishedNext(this, session, tunnel)
    }

    open fun onResponse(
        chain: HttpInterceptChain,
        buffer: ByteBuffer,
        session: HttpSession,
        tunnel: TcpTunnel,
        index: Int
    ) {
        chain.processResponseNext(this, buffer, session, tunnel)
    }

    open fun onResponseFinished(
        chain: HttpInterceptChain,
        session: HttpSession,
        tunnel: TcpTunnel,
        index: Int
    ) {
        chain.processResponseFinishedNext(this, session, tunnel)
    }

    @UnsupportedCall
    final override fun onRequest(
        chain: HttpInterceptChain,
        buffer: ByteBuffer,
        session: HttpSession,
        tunnel: TcpTunnel
    ) {
        reqIndexMap.compute(session) { _, value -> (value ?: -1) + 1 }
        onRequest(chain, buffer, session, tunnel, reqIndexMap[session] ?: return)
    }

    @UnsupportedCall
    final override fun onRequestFinished(
        chain: HttpInterceptChain,
        session: HttpSession,
        tunnel: TcpTunnel
    ) {
        onRequestFinished(chain, session, tunnel, (reqIndexMap[session] ?: return) + 1)
        reqIndexMap.remove(session)
    }

    @UnsupportedCall
    final override fun onResponse(
        chain: HttpInterceptChain,
        buffer: ByteBuffer,
        session: HttpSession,
        tunnel: TcpTunnel
    ) {
        rspIndexMap.compute(session) { _, value -> (value ?: -1) + 1 }
        onResponse(chain, buffer, session, tunnel, rspIndexMap[session] ?: return)
    }

    @UnsupportedCall
    final override fun onResponseFinished(
        chain: HttpInterceptChain,
        session: HttpSession,
        tunnel: TcpTunnel
    ) {
        onResponseFinished(chain, session, tunnel, (rspIndexMap[session] ?: return) + 1)
        rspIndexMap.remove(session)
    }

}