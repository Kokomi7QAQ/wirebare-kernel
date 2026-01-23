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

package top.sankokomi.wirebare.kernel.util

import android.util.Log
import androidx.annotation.IntDef
import top.sankokomi.wirebare.kernel.common.WireBare
import top.sankokomi.wirebare.kernel.net.IPVersion
import top.sankokomi.wirebare.kernel.net.TcpSession
import top.sankokomi.wirebare.kernel.net.UdpSession

object Level {
    const val VERBOSE = 1
    const val DEBUG = 1 shl 1
    const val INFO = 1 shl 2
    const val WARN = 1 shl 3
    const val ERROR = 1 shl 4
    const val WTF = 1 shl 5
    const val SILENT = 1 shl 6
}

@Target(
    AnnotationTarget.FIELD,
    AnnotationTarget.LOCAL_VARIABLE,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.VALUE_PARAMETER
)
@IntDef(value = [Level.VERBOSE, Level.DEBUG, Level.INFO, Level.WARN, Level.ERROR, Level.WTF, Level.SILENT])
annotation class LogLevel

internal object WireBareLogger {

    private const val TAG = "WireBare"

    @LogLevel
    @Volatile
    internal var LOG_LEVEL: Int = Level.VERBOSE

    internal fun verbose(msg: String) {
        if (LOG_LEVEL <= Level.VERBOSE) {
            Log.v(TAG, msg)
        }
    }

    internal fun debug(msg: String) {
        if (LOG_LEVEL <= Level.DEBUG) {
            Log.d(TAG, msg)
        }
    }

    internal fun info(msg: String) {
        if (LOG_LEVEL <= Level.INFO) {
            Log.i(TAG, msg)
        }
    }

    internal fun warn(msg: String) {
        if (LOG_LEVEL <= Level.WARN) {
            Log.w(TAG, msg)
        }
    }

    internal fun warn(cause: Throwable? = null) {
        if (LOG_LEVEL <= Level.WARN) {
            Log.w(TAG, cause?.message, cause)
        }
    }

    internal fun error(msg: String, cause: Throwable? = null) {
        if (LOG_LEVEL <= Level.ERROR) {
            Log.e(TAG, msg, cause)
        }
    }

    internal fun error(cause: Throwable? = null) {
        if (LOG_LEVEL <= Level.ERROR) {
            Log.e(TAG, cause?.message, cause)
        }
    }

    internal fun wtf(cause: Throwable) {
        if (LOG_LEVEL <= Level.WTF) {
            Log.wtf(TAG, cause)
        }
    }

    internal fun test(tag: String, msg: Any?) {
        Log.e(tag, msg.toString())
    }

    internal fun inetVerbose(session: TcpSession, msg: String) {
        verbose("${tcpPrefix(session)} >> ${session.destinationAddress}:${session.destinationPort} $msg")
    }

    internal fun inetDebug(session: TcpSession, msg: String) {
        debug("${tcpPrefix(session)} >> ${session.destinationAddress}:${session.destinationPort} $msg")
    }

    internal fun inetInfo(session: TcpSession, msg: String) {
        info("${tcpPrefix(session)} >> ${session.destinationAddress}:${session.destinationPort} $msg")
    }

    internal fun inetDebug(session: UdpSession, msg: String) {
        debug("[${session.protocol.name}] ${WireBare.configuration.ipv4Address}:${session.sourcePort} >> ${session.destinationAddress}:${session.destinationPort} $msg")
    }

    internal fun inetInfo(session: UdpSession, msg: String) {
        info("[${session.protocol.name}] ${WireBare.configuration.ipv4Address}:${session.sourcePort} >> ${session.destinationAddress}:${session.destinationPort} $msg")
    }

    private fun tcpPrefix(session: TcpSession): String {
        return when (session.destinationAddress.ipVersion) {
            IPVersion.IPv4 -> "[IPv4-TCP] ${session.sourcePort}"
            IPVersion.IPv6 -> "[IPv6-TCP] ${session.sourcePort}"
        }
    }

}
