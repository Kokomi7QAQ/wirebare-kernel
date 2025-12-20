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
import java.nio.ByteBuffer

class HttpInterceptChain(
    private val interceptors: List<HttpInterceptor>
) {

    private val interceptorIndexMap =
        hashMapOf<HttpInterceptor, HttpInterceptor?>().also { map ->
            interceptors.forEachIndexed { index, interceptor ->
                map[interceptor] = interceptors.getOrNull(index + 1)
            }
        }

    /**
     * 处理请求体
     * */
    fun processRequestNext(
        now: HttpInterceptor?,
        buffer: ByteBuffer,
        session: HttpSession,
        tunnel: TcpTunnel
    ) {
        nextInterceptor(now)?.onRequest(this, buffer, session, tunnel)
    }

    /**
     * 请求体处理完毕
     * */
    fun processRequestFinishedNext(
        now: HttpInterceptor?,
        session: HttpSession,
        tunnel: TcpTunnel
    ) {
        nextInterceptor(now)?.onRequestFinished(this, session, tunnel)
    }

    /**
     * 处理响应体
     * */
    fun processResponseNext(
        now: HttpInterceptor?,
        buffer: ByteBuffer,
        session: HttpSession,
        tunnel: TcpTunnel
    ) {
        nextInterceptor(now)?.onResponse(this, buffer, session, tunnel)
    }

    /**
     * 响应体处理完毕
     * */
    fun processResponseFinishedNext(
        now: HttpInterceptor?,
        session: HttpSession,
        tunnel: TcpTunnel
    ) {
        nextInterceptor(now)?.onResponseFinished(this, session, tunnel)
    }

    internal fun processRequestFirst(
        buffer: ByteBuffer,
        session: HttpSession,
        tunnel: TcpTunnel
    ) {
        processRequestNext(null, buffer, session, tunnel)
    }

    internal fun processRequestFinishedFirst(
        session: HttpSession,
        tunnel: TcpTunnel
    ) {
        processRequestFinishedNext(null, session, tunnel)
    }

    internal fun processResponseFirst(
        buffer: ByteBuffer,
        session: HttpSession,
        tunnel: TcpTunnel
    ) {
        processResponseNext(null, buffer, session, tunnel)
    }

    internal fun processResponseFinishedFirst(
        session: HttpSession,
        tunnel: TcpTunnel
    ) {
        processResponseFinishedNext(null, session, tunnel)
    }

    private fun nextInterceptor(now: HttpInterceptor?): HttpInterceptor? {
        now ?: return interceptors.firstOrNull()
        return interceptorIndexMap[now]
    }
}