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

package top.sankokomi.wirebare.kernel.interceptor.http.async

import kotlinx.coroutines.runBlocking
import top.sankokomi.wirebare.kernel.common.IProxyStatusListener
import top.sankokomi.wirebare.kernel.common.ProxyStatus
import top.sankokomi.wirebare.kernel.common.WireBare
import top.sankokomi.wirebare.kernel.interceptor.http.HttpInterceptChain
import top.sankokomi.wirebare.kernel.interceptor.http.HttpInterceptor
import top.sankokomi.wirebare.kernel.interceptor.http.HttpSession
import top.sankokomi.wirebare.kernel.interceptor.tcp.TcpTunnel
import top.sankokomi.wirebare.kernel.util.deepCopy
import java.nio.ByteBuffer
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

class AsyncHttpInterceptChain(
    private val interceptors: List<AsyncHttpInterceptor>
) : HttpInterceptor {

    override fun onRequest(
        chain: HttpInterceptChain,
        buffer: ByteBuffer,
        session: HttpSession,
        tunnel: TcpTunnel
    ) {
        processRequestFirst(buffer, session)
        super.onRequest(chain, buffer, session, tunnel)
    }

    override fun onRequestFinished(
        chain: HttpInterceptChain,
        session: HttpSession,
        tunnel: TcpTunnel
    ) {
        processRequestFinishedFirst(session)
        super.onRequestFinished(chain, session, tunnel)
    }

    override fun onResponse(
        chain: HttpInterceptChain,
        buffer: ByteBuffer,
        session: HttpSession,
        tunnel: TcpTunnel
    ) {
        processResponseFirst(buffer, session)
        super.onResponse(chain, buffer, session, tunnel)
    }

    override fun onResponseFinished(
        chain: HttpInterceptChain,
        session: HttpSession,
        tunnel: TcpTunnel
    ) {
        processResponseFinishedFirst(session)
        super.onResponseFinished(chain, session, tunnel)
    }

    private val interceptorIndexMap =
        hashMapOf<AsyncHttpInterceptor, AsyncHttpInterceptor?>().also { map ->
            interceptors.forEachIndexed { index, interceptor ->
                map[interceptor] = interceptors.getOrNull(index + 1)
            }
        }

    /**
     * 处理请求体
     * */
    suspend fun processRequestNext(
        now: AsyncHttpInterceptor?,
        buffer: ByteBuffer,
        session: HttpSession
    ) {
        nextInterceptor(now)?.onRequest(this, buffer, session)
    }

    /**
     * 请求体处理完毕
     * */
    suspend fun processRequestFinishedNext(
        now: AsyncHttpInterceptor?,
        session: HttpSession
    ) {
        nextInterceptor(now)?.onRequestFinished(this, session)
    }

    /**
     * 处理响应体
     * */
    suspend fun processResponseNext(
        now: AsyncHttpInterceptor?,
        buffer: ByteBuffer,
        session: HttpSession
    ) {
        nextInterceptor(now)?.onResponse(this, buffer, session)
    }

    /**
     * 响应体处理完毕
     * */
    suspend fun processResponseFinishedNext(
        now: AsyncHttpInterceptor?,
        session: HttpSession
    ) {
        nextInterceptor(now)?.onResponseFinished(this, session)
    }

    private val executorPool = ThreadPoolExecutor(
        1,
        1,
        Long.MAX_VALUE,
        TimeUnit.DAYS,
        LinkedBlockingQueue(),
        ThreadPoolExecutor.DiscardPolicy()
    ).also {
        WireBare.addVpnProxyStatusListener(
            object : IProxyStatusListener {
                override fun onVpnStatusChanged(oldStatus: ProxyStatus, newStatus: ProxyStatus): Boolean {
                    if (newStatus == ProxyStatus.DEAD) {
                        it.shutdown()
                        return true
                    }
                    return false
                }
            }
        )
    }

    private fun processRequestFirst(
        buffer: ByteBuffer,
        session: HttpSession
    ) {
        val buf = buffer.deepCopy()
        executorPool.execute {
            runBlocking {
                processRequestNext(null, buf, session)
            }
        }
    }

    private fun processRequestFinishedFirst(
        session: HttpSession
    ) {
        executorPool.execute {
            runBlocking {
                processRequestFinishedNext(null, session)
            }
        }
    }

    private fun processResponseFirst(
        buffer: ByteBuffer,
        session: HttpSession
    ) {
        val buf = buffer.deepCopy()
        executorPool.execute {
            runBlocking {
                processResponseNext(null, buf, session)
            }
        }
    }

    private fun processResponseFinishedFirst(
        session: HttpSession
    ) {
        executorPool.execute {
            runBlocking {
                processResponseFinishedNext(null, session)
            }
        }
    }

    private fun nextInterceptor(now: AsyncHttpInterceptor?): AsyncHttpInterceptor? {
        now ?: return interceptors.firstOrNull()
        return interceptorIndexMap[now]
    }
}