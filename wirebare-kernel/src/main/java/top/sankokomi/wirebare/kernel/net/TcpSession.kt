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
 * 会话，存储请求/响应的信息
 *
 * @param sourcePort 会话的来源端口号
 * @param destinationAddress 会话的目的 ipv4 地址
 * @param destinationPort 会话的目的端口号
 * @param sessionStore 会话所对应的 [TcpSessionStore]
 * */
data class TcpSession(
    override val sourcePort: Port,
    override val destinationAddress: IpAddress,
    override val destinationPort: Port,
    val sessionStore: TcpSessionStore
) : Session {

    override val protocol: Protocol = Protocol.TCP

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TcpSession

        if (sourcePort != other.sourcePort) return false
        if (destinationAddress != other.destinationAddress) return false
        return destinationPort == other.destinationPort
    }

    override fun hashCode(): Int {
        var result = sourcePort.hashCode()
        result = 31 * result + destinationAddress.hashCode()
        result = 31 * result + destinationPort.hashCode()
        return result
    }

    override fun toString(): String {
        return "{sourcePort = $sourcePort, " +
                "destinationAddress = $destinationAddress, " +
                "destinationPort = $destinationPort}"
    }
}
