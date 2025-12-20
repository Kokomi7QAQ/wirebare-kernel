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
import javax.net.ssl.SSLEngine

interface SSLCallback {
    /**
     * 要等待数据包完整后再处理，下次回调时希望将两个数据包合并后再传入
     * */
    fun shouldPending(target: ByteBuffer) {}

    /**
     * 解密失败，通常是因为没有创建出 [SSLEngine]
     * */
    fun sslFailed(target: ByteBuffer) {}

    /**
     * 解密完成
     * */
    fun decryptSuccess(target: ByteBuffer) {}

    /**
     * 加密完成
     * */
    fun encryptSuccess(target: ByteBuffer) {}
}