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

import java.io.Serializable

class HttpResponse internal constructor() : Serializable {

    companion object {
        private var sequence = 0L
    }

    val sequence: Long = synchronized(HttpResponse) { HttpResponse.sequence++ }

    var requestTime: Long? = null
        internal set

    /**
     * 来源端口号
     * */
    var sourcePort: Short? = null
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
    var url: String? = null
        internal set
    var isHttps: Boolean? = null
        internal set
    var httpVersion: String? = null
        internal set
    var rspStatus: String? = null
        internal set
    var originHead: String? = null
        internal set
    var formatHead: List<String>? = null
        internal set
    internal var hostInternal: String? = null
    internal var isPlaintext: Boolean? = null
    var host: String? = null
        internal set
    var contentType: String? = null
        internal set
    var contentEncoding: String? = null
        internal set
}