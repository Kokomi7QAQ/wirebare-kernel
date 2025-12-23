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

package top.sankokomi.wirebare.kernel.proxy

import kotlinx.coroutines.isActive
import top.sankokomi.wirebare.kernel.nio.NioCallback
import top.sankokomi.wirebare.kernel.util.WireBareLogger
import java.nio.channels.Selector

/**
 * 适用于 NIO 的代理服务器的抽象，已经实现了 [Selector.select] 操作并进行对应回调
 *
 * @see [ProxyServer]
 * @see [NioCallback]
 * */
internal abstract class NioProxyServer : ProxyServer() {

    /**
     * NIO 的选择器，已经在 [process] 中对其完成了 [Selector.select] 操作并进行对应回调
     * */
    protected abstract val selector: Selector

    final override suspend fun process() {
        var select = 0
        while (isActive) {
            select = selector.select()
            if (select != 0) {
                break
            }
        }
        if (select == 0) return
        val selectionKeys = selector.selectedKeys()
        var selectionKey = selectionKeys.firstOrNull()
        while (selectionKey != null) {
            val key = selectionKey
            selectionKeys.remove(key)
            selectionKey = selectionKeys.firstOrNull()
            val callback = key.attachment()
            if (!key.isValid || callback !is NioCallback) continue
            try {
                if (key.isAcceptable) callback.onAccept()
                else if (key.isConnectable) callback.onConnected()
                else if (key.isReadable) callback.onRead()
                else if (key.isWritable) callback.onWrite()
            } catch (e: Exception) {
                WireBareLogger.error("在 NIO KEY 处理时发生错误", e)
                callback.onException(e)
            }
        }
    }

}