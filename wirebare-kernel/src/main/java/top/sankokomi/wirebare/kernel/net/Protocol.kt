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

package top.sankokomi.wirebare.kernel.net

/**
 * 协议类
 *
 * @param name 协议名称
 * */
class Protocol private constructor(internal val name: String) {

    internal companion object {
        /**
         * 代表 TCP 协议
         * */
        val TCP = Protocol("TCP")

        /**
         * 代表 UDP 协议
         * */
        val UDP = Protocol("UDP")

        /**
         * 代表未知协议
         * */
        val NULL = Protocol("NULL")

        private val protocols = hashMapOf(
            6.toByte() to TCP,
            17.toByte() to UDP
        )

        /**
         * 根据协议对应的代码，取得对应协议
         *
         * @return 若支持该协议，则返回对应协议，否则返回 [NULL]
         *
         * @see [TCP]
         * @see [UDP]
         * @see [NULL]
         * */
        internal fun parse(code: Byte): Protocol {
            return protocols[code] ?: NULL
        }
    }

}