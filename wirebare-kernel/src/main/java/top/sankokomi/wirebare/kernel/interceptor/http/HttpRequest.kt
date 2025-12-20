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

@file:Suppress("PropertyName")

package top.sankokomi.wirebare.kernel.interceptor.http

import java.io.Serializable

/**
 * 请求信息
 * */
class HttpRequest internal constructor() : Serializable {

    companion object {
        private var sequence = 0L
    }

    val sequence: Long = synchronized(HttpRequest) { HttpRequest.sequence++ }

    var requestTime: Long? = null
        internal set

    /**
     * 来源端口号
     * */
    var sourcePort: Short? = null
        internal set

    var sourcePkgName: String? = null
        internal set

    /**
     * 目的地址
     * */
    var destinationAddress: String? = null
        internal set

    /**
     * 目的端口号
     * */
    var destinationPort: Short? = null
        internal set

    /**
     * 请求的方法，需要是 HTTP/HTTPS 协议才可以解析
     * */
    var method: String? = null
        internal set

    /**
     * true 表示当前请求为 HTTP 请求
     *
     * false 表示当前请求为 HTTPS 请求
     *
     * null 表示未知，既不是 HTTP 也不是 HTTPS
     * */
    var isHttps: Boolean? = null
        internal set

    /**
     * 若为 HTTP/HTTPS 请求，则为 HTTP 版本，否则为 null
     * */
    var httpVersion: String? = null
        internal set

    /**
     * [isHttps] == true 时该值才有效
     *
     * true 表示已经完成 SSL/TLS 的握手流程，拦截器中拿到的都是明文
     * */
    internal var isPlaintext: Boolean? = null

    internal var hostInternal: String? = null

    /**
     * 请求的域名
     * */
    var host: String? = null
        internal set

    /**
     * 请求的路径
     * */
    var path: String? = null
        internal set

    /**
     * 原始的请求头，包含的是最原始的请求头信息
     * */
    var originHead: String? = null
        internal set

    /**
     * 整个请求头，已经以 \r\n 为间隔分隔好
     * */
    var formatHead: List<String>? = null
        internal set

    /**
     * 请求的 URL ，需要是 HTTP/HTTPS 协议才可以解析
     * */
    val url: String?
        get() {
            if (host == null || path == null) {
                return null
            }
            return when (isHttps) {
                false -> "http://${host}${path}"
                true -> "https://${host}${path}"
                else -> null
            }
        }

}
