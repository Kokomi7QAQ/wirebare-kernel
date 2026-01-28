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

import top.sankokomi.wirebare.kernel.net.TcpSession
import top.sankokomi.wirebare.kernel.util.readUnsignedByte
import top.sankokomi.wirebare.kernel.util.readUnsignedShort
import java.nio.ByteBuffer

abstract class SSLCodec {

    abstract fun createSSLEngineWrapper(session: TcpSession, host: String): WireBareSSLEngine?

    internal fun decode(
        session: TcpSession,
        host: String,
        buffer: ByteBuffer,
        callback: SSLCallback
    ) {
        when (verifyPacket(buffer)) {
            VerifyResult.NotEncrypted -> {
                callback.decryptSuccess(buffer)
            }

            VerifyResult.NotEnough -> {
                callback.shouldPending(buffer)
            }

            VerifyResult.Ready -> {
                realDecode(session, host, buffer, callback)
            }
        }
    }

    internal fun encode(
        session: TcpSession,
        host: String,
        buffer: ByteBuffer,
        callback: SSLCallback
    ) {
        realEncode(session, host, buffer, callback)
    }

    private fun realDecode(
        session: TcpSession,
        host: String,
        buffer: ByteBuffer,
        callback: SSLCallback
    ) {
        val engine = createSSLEngineWrapper(session, host)
        if (engine == null) {
            callback.sslFailed(buffer)
            return
        }
        engine.decodeBuffer(buffer, callback)
    }

    private fun realEncode(
        session: TcpSession,
        host: String,
        buffer: ByteBuffer,
        callback: SSLCallback
    ) {
        val engine = createSSLEngineWrapper(session, host)
        if (engine == null) {
            // 按理来说这里应该不可能进来
            callback.sslFailed(buffer)
            return
        }
        engine.encodeBuffer(buffer, callback)
    }

    enum class VerifyResult {
        /**
         * 数据未完整，等待下一个数据包再做解密
         * */
        NotEnough,

        /**
         * 数据包是明文，不需要解密
         * */
        NotEncrypted,

        /**
         * 数据包准备就绪，可以进行解密
         * */
        Ready
    }

    private fun verifyPacket(buffer: ByteBuffer): VerifyResult {
        val position = buffer.position()
        if (buffer.remaining() < SSLPredicate.SSL_RECORD_HEADER_LENGTH) {
            return VerifyResult.NotEnough
        }
        var packetLength = 0
        var tls = buffer.readUnsignedByte(position) in SSLPredicate.httpsConnectHeadSet
        if (tls) {
            val majorVersion = buffer.readUnsignedByte(position + 1)
            if (majorVersion == 3) {
                packetLength = buffer.readUnsignedShort(
                    position + 3
                ) + SSLPredicate.SSL_RECORD_HEADER_LENGTH
                if (packetLength <= SSLPredicate.SSL_RECORD_HEADER_LENGTH) {
                    tls = false
                }
            } else {
                tls = false
            }
        }
        if (!tls) {
            val headerLength = if (
                buffer.readUnsignedByte(
                    position
                ) and 0x80 != 0
            ) 2 else 3
            val majorVersion: Int = buffer.readUnsignedByte(position + headerLength + 1)
            if (majorVersion == 2 || majorVersion == 3) {
                packetLength = if (headerLength == 2) {
                    (buffer.getShort(position).toInt() and 0x7FFF) + 2
                } else {
                    (buffer.getShort(position).toInt() and 0x3FFF) + 3
                }
                if (packetLength <= headerLength) {
                    return VerifyResult.NotEnough
                }
            } else {
                return VerifyResult.NotEncrypted
            }
        }
        if (packetLength > buffer.remaining()) {
            return VerifyResult.NotEnough
        }
        return VerifyResult.Ready
    }

}