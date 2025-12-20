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

    internal const val HTTP_METHOD_GET = 'G'.code
    internal const val HTTP_METHOD_HEAD = 'H'.code
    internal const val HTTP_METHOD_POST_PUT_PATCH = 'P'.code
    internal const val HTTP_METHOD_DELETE = 'D'.code
    internal const val HTTP_METHOD_OPTIONS = 'O'.code
    internal const val HTTP_METHOD_TRACE = 'T'.code
    internal const val HTTP_METHOD_CONNECT = 'C'.code

    internal const val SSL_CONTENT_TYPE_CHANGE_CIPHER_SPEC = 20
    internal const val SSL_CONTENT_TYPE_ALERT = 21
    internal const val SSL_CONTENT_TYPE_HANDSHAKE = 22
    internal const val SSL_CONTENT_TYPE_APPLICATION_DATA = 23
    internal const val SSL_CONTENT_TYPE_EXTENSION_HEARTBEAT = 24
}

internal val ByteBuffer.judgeIsHttps: Boolean?
    get() {
        if (!hasRemaining()) return null
        return when (get(position()).toInt()) {
            SSLPredicate.HTTP_METHOD_GET,/* GET */
            SSLPredicate.HTTP_METHOD_HEAD,/* HEAD */
            SSLPredicate.HTTP_METHOD_POST_PUT_PATCH,/* POST, PUT, PATCH */
            SSLPredicate.HTTP_METHOD_DELETE,/* DELETE */
            SSLPredicate.HTTP_METHOD_OPTIONS,/* OPTIONS */
            SSLPredicate.HTTP_METHOD_TRACE,/* TRACE */
            SSLPredicate.HTTP_METHOD_CONNECT/* CONNECT */ -> false

            /* HTTPS */
            SSLPredicate.SSL_CONTENT_TYPE_CHANGE_CIPHER_SPEC,
            SSLPredicate.SSL_CONTENT_TYPE_ALERT,
            SSLPredicate.SSL_CONTENT_TYPE_HANDSHAKE,
            SSLPredicate.SSL_CONTENT_TYPE_APPLICATION_DATA,
            SSLPredicate.SSL_CONTENT_TYPE_EXTENSION_HEARTBEAT -> true

            else -> null
        }
    }