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
import top.sankokomi.wirebare.kernel.net.Session

object WireBareLogger {

    @Volatile
    internal var level: Level = Level.VERBOSE

    internal var proxy: Proxy = DefaultProxy

    internal fun verbose(tag: String, msg: String, cause: Throwable? = null) {
        if (level.value <= Level.VERBOSE.value) {
            proxy.verbose(tag, msg, cause)
        }
    }

    internal fun debug(tag: String, msg: String, cause: Throwable? = null) {
        if (level.value <= Level.DEBUG.value) {
            proxy.debug(tag, msg, cause)
        }
    }

    internal fun info(tag: String, msg: String, cause: Throwable? = null) {
        if (level.value <= Level.INFO.value) {
            proxy.info(tag, msg, cause)
        }
    }

    internal fun warn(tag: String, msg: String, cause: Throwable? = null) {
        if (level.value <= Level.WARN.value) {
            proxy.warn(tag, msg, cause)
        }
    }

    internal fun error(tag: String, msg: String, cause: Throwable? = null) {
        if (level.value <= Level.ERROR.value) {
            proxy.error(tag, msg, cause)
        }
    }

    internal fun wtf(tag: String, msg: String, cause: Throwable? = null) {
        if (level.value <= Level.WTF.value) {
            proxy.error(tag, msg, cause)
        }
    }

    internal fun inetVerbose(tag: String, session: Session, msg: String, cause: Throwable? = null) {
        verbose(tag, "${inetPrefix(session)} $msg", cause)
    }

    internal fun inetDebug(tag: String, session: Session, msg: String, cause: Throwable? = null) {
        debug(tag, "${inetPrefix(session)} $msg", cause)
    }

    internal fun inetInfo(tag: String, session: Session, msg: String, cause: Throwable? = null) {
        info(tag, "${inetPrefix(session)} $msg", cause)
    }

    internal fun inetWarn(tag: String, session: Session, msg: String, cause: Throwable? = null) {
        warn(tag, "${inetPrefix(session)} $msg", cause)
    }

    internal fun inetError(tag: String, session: Session, msg: String, cause: Throwable? = null) {
        error(tag, "${inetPrefix(session)} $msg", cause)
    }

    internal fun inetWtf(tag: String, session: Session, msg: String, cause: Throwable? = null) {
        wtf(tag, "${inetPrefix(session)} $msg", cause)
    }

    private fun inetPrefix(session: Session): String {
        val ipVersionName = session.destinationAddress.ipVersion.versionName
        val protocolName = session.protocol.name
        val sourcePort = session.sourcePort
        val destinationAddress = session.destinationAddress
        val destinationPort = session.destinationPort
        return "[$ipVersionName-$protocolName] [$sourcePort <> $destinationAddress:$destinationPort]"
    }

    enum class Level(
        val value: Int
    ) {
        VERBOSE(1),
        DEBUG(1 shl 1),
        INFO(1 shl 2),
        WARN(1 shl 3),
        ERROR(1 shl 4),
        WTF(1 shl 5),
        SILENT(1 shl 6)
    }

    interface Proxy {
        fun verbose(tag: String, msg: String, cause: Throwable?)
        fun debug(tag: String, msg: String, cause: Throwable?)
        fun info(tag: String, msg: String, cause: Throwable?)
        fun warn(tag: String, msg: String, cause: Throwable?)
        fun error(tag: String, msg: String, cause: Throwable?)
        fun wtf(tag: String, msg: String, cause: Throwable?)
    }

    object DefaultProxy : Proxy {
        override fun verbose(tag: String, msg: String, cause: Throwable?) {
            Log.v(tag, msg, cause)
        }

        override fun debug(tag: String, msg: String, cause: Throwable?) {
            Log.d(tag, msg, cause)
        }

        override fun info(tag: String, msg: String, cause: Throwable?) {
            Log.i(tag, msg, cause)
        }

        override fun warn(tag: String, msg: String, cause: Throwable?) {
            Log.w(tag, msg, cause)
        }

        override fun error(tag: String, msg: String, cause: Throwable?) {
            Log.e(tag, msg, cause)
        }

        override fun wtf(tag: String, msg: String, cause: Throwable?) {
            Log.wtf(tag, msg, cause)
        }

    }

}
