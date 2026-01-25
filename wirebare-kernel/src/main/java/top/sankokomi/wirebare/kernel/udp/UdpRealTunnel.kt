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

package top.sankokomi.wirebare.kernel.udp

import android.net.VpnService
import top.sankokomi.wirebare.kernel.common.EventSynopsis
import top.sankokomi.wirebare.kernel.common.ImportantEvent
import top.sankokomi.wirebare.kernel.common.WireBare
import top.sankokomi.wirebare.kernel.common.WireBareConfiguration
import top.sankokomi.wirebare.kernel.net.IPHeader
import top.sankokomi.wirebare.kernel.net.IPVersion
import top.sankokomi.wirebare.kernel.net.UdpHeader
import top.sankokomi.wirebare.kernel.net.UdpSession
import top.sankokomi.wirebare.kernel.nio.DatagramSocketNioTunnel
import top.sankokomi.wirebare.kernel.util.WireBareLogger
import top.sankokomi.wirebare.kernel.util.closeSafely
import top.sankokomi.wirebare.kernel.util.ipVersion
import java.io.OutputStream
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import java.nio.channels.Selector

/**
 * UDP 代理客户端
 *
 * 负责与远程服务器通信，发送 UDP 数据包到远程服务器，并接收远程服务器返回的 UDP 数据包
 * */
internal class UdpRealTunnel(
    override val channel: DatagramChannel,
    override val selector: Selector,
    private val outputStream: OutputStream,
    private val session: UdpSession,
    udpHeader: UdpHeader,
    private val configuration: WireBareConfiguration,
    private val vpnService: VpnService
) : DatagramSocketNioTunnel() {

    companion object {
        private const val TAG = "UdpRealTunnel"
    }

    private val header: UdpHeader = udpHeader.copy().also {
        val localAddress = it.ipHeader.sourceAddress
        val localPort = it.sourcePort
        val remoteAddress = it.ipHeader.destinationAddress
        val remotePort = it.destinationPort
        it.ipHeader.sourceAddress = remoteAddress
        it.sourcePort = remotePort
        it.ipHeader.destinationAddress = localAddress
        it.destinationPort = localPort
    }

    internal fun connectRemoteServer(address: String, port: Int) {
        if (vpnService.protect(channel.socket())) {
            channel.configureBlocking(false)
            WireBareLogger.warn(TAG, "start to connect remote server($address:$port)")
            try {
                channel.connect(InetSocketAddress(address, port))
            } catch (e: Exception) {
                reportExceptionWhenConnect(address, port, e)
                WireBareLogger.error(
                    TAG,
                    "connect to remote server($address:$port) failed",
                    e
                )
                onException(e)
            }
            prepareRead()
        } else {
            throw IllegalArgumentException("cannot protect datagram socket for udp tunnel")
        }
    }

    override fun onWrite(): Int {
        val length = super.onWrite()
        WireBareLogger.inetDebug(TAG, session, "proxy client write $length bytes")
        return length
    }

    override fun onRead() {
        val buffer = ByteBuffer.allocate(configuration.mtu)
        val length = read(buffer)
        if (length < 0) {
            closeSafely()
        } else {
            WireBareLogger.inetDebug(TAG, session, "proxy client read $length bytes")
            outputStream.write(createUdpMessage(buffer))
        }
    }

    /**
     * 创建 UDP 报文，此 UDP 报文的来源是远程服务器，目的地是被代理客户端，数据部分为 [buffer]
     * */
    private fun createUdpMessage(buffer: ByteBuffer): ByteArray {
        val arrayLength = header.ipHeader.headerLength + 8 + buffer.remaining()

        val packet = ByteArray(arrayLength) {
            if (it < header.ipHeader.headerLength + 8) {
                header.packet[it]
            } else {
                buffer[it - header.ipHeader.headerLength - 8]
            }
        }
        val ipHeader = IPHeader.parse(packet, arrayLength, 0)!!
        val udpHeader = UdpHeader(ipHeader, packet, ipHeader.headerLength)
        ipHeader.totalLength = arrayLength
        udpHeader.totalLength = arrayLength - ipHeader.headerLength
        ipHeader.notifyCheckSum()
        udpHeader.notifyCheckSum()

        return packet
    }

    private fun reportExceptionWhenConnect(address: String, port: Int, t: Throwable?) {
        when (address.ipVersion) {
            IPVersion.IPv4 -> {
                WireBare.postImportantEvent(
                    ImportantEvent(
                        "[UDP] try to connect to $address:$port failed",
                        EventSynopsis.IPV4_UNREACHABLE,
                        t
                    )
                )
            }

            IPVersion.IPv6 -> {
                WireBare.postImportantEvent(
                    ImportantEvent(
                        "[UDP] try to connect to $address:$port failed",
                        EventSynopsis.IPV6_UNREACHABLE,
                        t
                    )
                )
            }

            else -> {}
        }
    }

    override fun onException(t: Throwable) {
        close()
    }

    override fun close() {
        super.close()
        WireBareLogger.inetDebug(TAG, session, "close")
    }

}