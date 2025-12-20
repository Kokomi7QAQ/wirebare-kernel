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

import top.sankokomi.wirebare.kernel.common.WireBareConfiguration
import top.sankokomi.wirebare.kernel.interceptor.http.async.AsyncHttpHeaderParserInterceptor
import top.sankokomi.wirebare.kernel.interceptor.http.async.AsyncHttpInterceptChain
import top.sankokomi.wirebare.kernel.interceptor.ssl.HttpSSLCodecInterceptor
import top.sankokomi.wirebare.kernel.interceptor.ssl.HttpSSLSniffInterceptor
import top.sankokomi.wirebare.kernel.interceptor.tcp.TcpTunnel
import java.nio.ByteBuffer

/**
 * 虚拟网关
 * */
class HttpVirtualGateway internal constructor(
    configuration: WireBareConfiguration
) {

    private val interceptorChain: HttpInterceptChain

    init {
        val interceptors = mutableListOf<HttpInterceptor>()
        // HTTP/HTTPS 嗅探拦截器，用于判断 HTTP/HTTPS
        interceptors.add(HttpSSLSniffInterceptor())
        val jks = configuration.jks
        if (jks != null) {
            val sslDecodeInterceptor = HttpSSLCodecInterceptor(jks)
            interceptors.add(sslDecodeInterceptor)
            interceptors.addNormalInterceptors(configuration)
            interceptors.add(
                HttpFlushInterceptor(
                    sslDecodeInterceptor.requestCodec,
                    sslDecodeInterceptor.responseCodec
                )
            )
        } else {
            interceptors.addNormalInterceptors(configuration)
            interceptors.add(HttpFlushInterceptor())
        }
        interceptorChain = HttpInterceptChain(interceptors)
    }

    private fun MutableList<HttpInterceptor>.addNormalInterceptors(
        configuration: WireBareConfiguration,
    ) {
        val interceptors = this@addNormalInterceptors
        // 自定义拦截器
        interceptors.addAll(
            configuration.httpInterceptorFactories.map { it.create() }
        )
        interceptors.add(
            AsyncHttpInterceptChain(
                configuration.asyncHttpInterceptorFactories.mapTo(
                    // HTTP 请求头，响应头格式化拦截器
                    mutableListOf(AsyncHttpHeaderParserInterceptor())
                ) { it.create() }
            )
        )
    }

    fun onRequest(
        buffer: ByteBuffer,
        session: HttpSession,
        tunnel: TcpTunnel
    ) {
        interceptorChain.processRequestFirst(buffer, session, tunnel)
    }

    fun onRequestFinished(
        session: HttpSession,
        tunnel: TcpTunnel
    ) {
        interceptorChain.processRequestFinishedFirst(session, tunnel)
    }

    fun onResponse(
        buffer: ByteBuffer,
        session: HttpSession,
        tunnel: TcpTunnel
    ) {
        interceptorChain.processResponseFirst(buffer, session, tunnel)
    }

    fun onResponseFinished(
        session: HttpSession,
        tunnel: TcpTunnel
    ) {
        interceptorChain.processResponseFinishedFirst(session, tunnel)
    }

}