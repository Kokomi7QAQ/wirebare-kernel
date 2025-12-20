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

package top.sankokomi.wirebare.kernel.interceptor.tcp

import top.sankokomi.wirebare.kernel.net.TcpSession
import java.nio.ByteBuffer

class TcpInterceptChain(
    private val interceptors: List<TcpInterceptor>
) {

    private var interceptorIndex = -1

    /**
     * 处理请求体
     * */
    fun processRequestNext(
        buffer: ByteBuffer,
        session: TcpSession,
        tunnel: TcpTunnel
    ) {
        interceptorIndex++
        interceptors.getOrNull(
            interceptorIndex
        )?.onRequest(this, buffer, session, tunnel)
    }

    /**
     * 请求体处理完毕
     * */
    fun processRequestFinishedNext(
        session: TcpSession,
        tunnel: TcpTunnel
    ) {
        interceptorIndex++
        interceptors.getOrNull(
            interceptorIndex
        )?.onRequestFinished(this, session, tunnel)
    }

    /**
     * 处理响应体
     * */
    fun processResponseNext(
        buffer: ByteBuffer,
        session: TcpSession,
        tunnel: TcpTunnel
    ) {
        interceptorIndex++
        interceptors.getOrNull(
            interceptorIndex
        )?.onResponse(this, buffer, session, tunnel)
    }

    /**
     * 响应体处理完毕
     * */
    fun processResponseFinishedNext(
        session: TcpSession,
        tunnel: TcpTunnel
    ) {
        interceptorIndex++
        interceptors.getOrNull(
            interceptorIndex
        )?.onResponseFinished(this, session, tunnel)
    }

    internal fun processRequestFirst(
        buffer: ByteBuffer,
        session: TcpSession,
        tunnel: TcpTunnel
    ) {
        interceptorIndex = -1
        processRequestNext(buffer, session, tunnel)
    }

    internal fun processRequestFinishedFirst(
        session: TcpSession,
        tunnel: TcpTunnel
    ) {
        interceptorIndex = -1
        processRequestFinishedNext(session, tunnel)
    }

    internal fun processResponseFirst(
        buffer: ByteBuffer,
        session: TcpSession,
        tunnel: TcpTunnel
    ) {
        interceptorIndex = -1
        processResponseNext(buffer, session, tunnel)
    }

    internal fun processResponseFinishedFirst(
        session: TcpSession,
        tunnel: TcpTunnel
    ) {
        interceptorIndex = -1
        processResponseFinishedNext(session, tunnel)
    }
}