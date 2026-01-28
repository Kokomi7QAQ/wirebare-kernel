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

package top.sankokomi.wirebare.kernel.ssl

import java.nio.ByteBuffer

object SSLPredicate {
    internal const val SSL_RECORD_HEADER_LENGTH = 5

    private const val HTTP_METHOD_GET = "GET"
    private const val HTTP_METHOD_HEAD = "HEAD"
    private const val HTTP_METHOD_POST = "POST"
    private const val HTTP_METHOD_PUT = "PUT"
    private const val HTTP_METHOD_PATCH = "PATCH"
    private const val HTTP_METHOD_DELETE = "DELETE"
    private const val HTTP_METHOD_OPTIONS = "OPTIONS"
    private const val HTTP_METHOD_TRACE = "TRACE"
    private const val HTTP_METHOD_CONNECT = "CONNECT"

    private const val SSL_CONTENT_TYPE_CHANGE_CIPHER_SPEC = 20
    private const val SSL_CONTENT_TYPE_ALERT = 21
    private const val SSL_CONTENT_TYPE_HANDSHAKE = 22
    private const val SSL_CONTENT_TYPE_APPLICATION_DATA = 23
    private const val SSL_CONTENT_TYPE_EXTENSION_HEARTBEAT = 24

    val httpMethodSet = hashSetOf(
        HTTP_METHOD_GET,
        HTTP_METHOD_HEAD,
        HTTP_METHOD_POST,
        HTTP_METHOD_PUT,
        HTTP_METHOD_PATCH,
        HTTP_METHOD_DELETE,
        HTTP_METHOD_OPTIONS,
        HTTP_METHOD_TRACE,
        HTTP_METHOD_CONNECT
    )

    val httpMethodHeadSet = httpMethodSet.mapTo(hashSetOf()) {
        it.first().code
    }

    val httpsConnectHeadSet = hashSetOf(
        SSL_CONTENT_TYPE_CHANGE_CIPHER_SPEC,
        SSL_CONTENT_TYPE_ALERT,
        SSL_CONTENT_TYPE_HANDSHAKE,
        SSL_CONTENT_TYPE_APPLICATION_DATA,
        SSL_CONTENT_TYPE_EXTENSION_HEARTBEAT
    )
}

internal fun ByteBuffer.isHttpsPacket(): Boolean? {
    if (!hasRemaining()) return null
    val head = get(position()).toInt()
    return when (head) {
        in SSLPredicate.httpMethodHeadSet -> false
        in SSLPredicate.httpsConnectHeadSet -> true
        else -> null
    }
}