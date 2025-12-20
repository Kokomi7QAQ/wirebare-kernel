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

import top.sankokomi.wirebare.kernel.interceptor.http.async.AsyncHttpInterceptor
import top.sankokomi.wirebare.kernel.interceptor.tcp.TcpTunnel
import java.nio.ByteBuffer

/**
 * HTTP 阻塞拦截器，支持对报文进行修改，但会延长响应的耗时
 *
 * @see [AsyncHttpInterceptor]
 * */
interface HttpInterceptor {
    fun onRequest(
        chain: HttpInterceptChain,
        buffer: ByteBuffer,
        session: HttpSession,
        tunnel: TcpTunnel
    ) {
        chain.processRequestNext(this, buffer, session, tunnel)
    }

    fun onRequestFinished(
        chain: HttpInterceptChain,
        session: HttpSession,
        tunnel: TcpTunnel
    ) {
        chain.processRequestFinishedNext(this, session, tunnel)
    }

    fun onResponse(
        chain: HttpInterceptChain,
        buffer: ByteBuffer,
        session: HttpSession,
        tunnel: TcpTunnel
    ) {
        chain.processResponseNext(this, buffer, session, tunnel)
    }

    fun onResponseFinished(
        chain: HttpInterceptChain,
        session: HttpSession,
        tunnel: TcpTunnel
    ) {
        chain.processResponseFinishedNext(this, session, tunnel)
    }
}