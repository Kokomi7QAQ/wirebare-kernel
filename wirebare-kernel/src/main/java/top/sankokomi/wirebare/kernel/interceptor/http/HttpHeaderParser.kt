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

import top.sankokomi.wirebare.kernel.util.WireBareLogger
import top.sankokomi.wirebare.kernel.util.newString
import java.nio.ByteBuffer

internal fun parseHttpRequestHeader(
    buffer: ByteBuffer,
    session: HttpSession
) {
    try {
        val (request, _) = session
        val requestString = buffer.newString()
        val headerString = requestString.substringBefore("\r\n\r\n")
        val headers = headerString.split("\r\n")
        val requestLine = headers[0].split(" ".toRegex())
        request.originHead = requestString
        request.formatHead = headers.filter { it.isNotBlank() }
        request.method = requestLine[0]
        request.httpVersion = requestLine[requestLine.size - 1]
        request.path = headers[0].replace(requestLine[0], "")
            .replace(requestLine[requestLine.size - 1], "")
            .trim()
        parseHeaderLine(headers, listOf("Host: ")) { name, content ->
            when (name) {
                "Host: " -> request.host = content
            }
        }
    } catch (e: Exception) {
        WireBareLogger.error("构造 HTTP 请求 时出现错误", e)
    }
}

internal fun parseHttpResponseHeader(
    buffer: ByteBuffer,
    session: HttpSession
) {
    try {
        val (request, response) = session
        response.url = request.url
        val responseString = buffer.newString()
        val headerString = responseString.substringBefore("\r\n\r\n")
        val headers = headerString.split("\r\n")
        val responseLine = headers[0].split(" ".toRegex())
        response.originHead = headerString
        response.formatHead = headers.filter { it.isNotBlank() }
        response.httpVersion = responseLine[0]
        response.rspStatus = responseLine[1]
        parseHeaderLine(
            headers,
            listOf(
                "Content-Type: ",
                "Content-Encoding: "
            )
        ) { name, content ->
            when (name) {
                "Content-Type: " -> response.contentType = content
                "Content-Encoding: " -> response.contentEncoding = content
            }
        }
    } catch (e: Exception) {
        WireBareLogger.error("构造 HTTP 响应时出现错误", e)
    }
}

private fun parseHeaderLine(
    headers: List<String>,
    names: List<String>,
    onFound: (name: String, content: String) -> Unit
) {
    val nameList = names.toMutableList()
    headers.forEach { msg ->
        nameList.removeAll { name ->
            val index = msg.indexOf(name)
            if (index != -1) {
                onFound(name, msg.substring(index + name.length))
                return@removeAll true
            }
            return@removeAll false
        }
    }
}