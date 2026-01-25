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

package top.sankokomi.wirebare.kernel.tcp

import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import top.sankokomi.wirebare.kernel.common.BandwidthLimiter
import top.sankokomi.wirebare.kernel.common.WireBare
import top.sankokomi.wirebare.kernel.common.WireBareConfiguration
import top.sankokomi.wirebare.kernel.dashboard.BandwidthStat
import top.sankokomi.wirebare.kernel.dashboard.WireBareDashboard
import top.sankokomi.wirebare.kernel.interceptor.tcp.TcpVirtualGateway
import top.sankokomi.wirebare.kernel.net.IPHeader
import top.sankokomi.wirebare.kernel.net.IPVersion
import top.sankokomi.wirebare.kernel.net.IpAddress
import top.sankokomi.wirebare.kernel.net.Packet
import top.sankokomi.wirebare.kernel.net.Port
import top.sankokomi.wirebare.kernel.net.TcpHeader
import top.sankokomi.wirebare.kernel.net.TcpSessionStore
import top.sankokomi.wirebare.kernel.service.PacketInterceptor
import top.sankokomi.wirebare.kernel.service.WireBareProxyService
import top.sankokomi.wirebare.kernel.util.WireBareLogger
import top.sankokomi.wirebare.kernel.util.convertPortToInt
import java.io.OutputStream
import java.util.Queue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * TCP 报文拦截器
 *
 * 拦截 IP 包并修改 [IPHeader.sourceAddress] 、
 * [TcpHeader.sourcePort] 、 [IPHeader.destinationAddress] 、
 * [TcpHeader.destinationPort]
 *
 * 目的是将被代理客户端的请求数据包代理到 [TcpProxyServer] ，
 * 并将远程服务器的响应数据包转发给被代理客户端
 * */
internal class TcpPacketInterceptor(
    private val configuration: WireBareConfiguration,
    proxyService: WireBareProxyService
) : PacketInterceptor, CoroutineScope by proxyService {

    companion object {
        private const val TAG = "TcpPacketInterceptor"
    }

    private class PendingPacket(
        val packet: Packet,
        val ipHeader: IPHeader,
        val tcpHeader: TcpHeader,
        val outputStream: OutputStream,
        val time: Long = SystemClock.elapsedRealtime()
    )

    private val sessionStore: TcpSessionStore = TcpSessionStore()

    private val bandwidthStat = BandwidthStat(WireBareDashboard.mutableBandwidthFlow, proxyService)
    private val reqBandwidthStat =
        BandwidthStat(WireBareDashboard.mutableReqBandwidthFlow, proxyService)
    private val rspBandwidthStat =
        BandwidthStat(WireBareDashboard.mutableRspBandwidthFlow, proxyService)

    /**
     * 虚拟网卡的 ip 地址，也就是代理服务器的 ip 地址
     * */
    private val tunIPv4Address = IpAddress(
        configuration.ipv4Address,
        IPVersion.IPv4
    )

    private val tunIPv6Address = IpAddress(
        configuration.ipv6Address,
        IPVersion.IPv6
    )

    /**
     * 代理服务器的端口集合
     * */
    internal val ports = hashSetOf<Port>()

    /**
     * 代理服务器
     * */
    private val servers = mutableListOf<TcpProxyServer>().also { serverList ->
        repeat(configuration.tcpProxyServerCount) {
            val server = TcpProxyServer(
                sessionStore,
                TcpVirtualGateway(configuration),
                configuration,
                proxyService
            )
            server.dispatch()
            ports.add(server.proxyServerPort)
            serverList.add(server)
        }
    }

    private val lock = ReentrantLock(true)
    private val condition = lock.newCondition()
    private val reqPacketQueue = LinkedBlockingQueue<PendingPacket>()
    private val rspPacketQueue = LinkedBlockingQueue<PendingPacket>()
    private var waitingReqTransmit = AtomicBoolean(false)
    private var waitingRspTransmit = AtomicBoolean(false)

    init {
        launch(Dispatchers.IO) {
            while (isActive) {
                lock.withLock {
                    condition.await()
                }
                transmitPacket()
            }
        }
    }

    private fun transmitPacket() {
        if (!waitingReqTransmit.get()) {
            tryTransmit(
                "REQ",
                reqPacketQueue,
                WireBare.dynamicConfig.reqBandwidthLimiter,
                waitingReqTransmit
            )
        }
        if (!waitingRspTransmit.get()) {
            tryTransmit(
                "RSP",
                rspPacketQueue,
                WireBare.dynamicConfig.rspBandwidthLimiter,
                waitingRspTransmit
            )
        }
    }

    private fun tryTransmit(
        type: String,
        packetQueue: Queue<PendingPacket>,
        maxBandwidthLimiter: BandwidthLimiter,
        waitingTransmit: AtomicBoolean
    ) {
        while (packetQueue.isNotEmpty()) {
            val pendingPacket = packetQueue.peek() ?: break
            if (maxBandwidthLimiter.checkTimeout(pendingPacket.time)) {
                // 超时了，下一个
                packetQueue.poll()
            } else {
                val nextCanTransmitDelay =
                    maxBandwidthLimiter.nextCanTransmit(pendingPacket.packet.length)
                if (nextCanTransmitDelay > 0L) {
                    // 需要等待配额足够
                    WireBareLogger.error(
                        TAG,
                        "[$type] bandwidth limit, should wait $nextCanTransmitDelay ms"
                    )
                    waitingTransmit.set(true)
                    launch(Dispatchers.IO) {
                        delay(nextCanTransmitDelay)
                        WireBareLogger.error(TAG, "[$type] bandwidth enough restart")
                        waitingTransmit.set(false)
                        lock.withLock {
                            condition.signal()
                        }
                    }
                    break
                } else {
                    // 可以立即发送
                    packetQueue.poll()
                    transmit(
                        pendingPacket.packet,
                        pendingPacket.ipHeader,
                        pendingPacket.tcpHeader,
                        pendingPacket.outputStream
                    )
                    waitingTransmit.set(false)
                }
            }
        }
    }

    override fun intercept(
        ipHeader: IPHeader,
        packet: Packet,
        outputStream: OutputStream
    ) {
        if (ipHeader.ipVersion == IPVersion.IPv6 && !configuration.enableIPv6) {
            WireBareLogger.error(TAG, "IPv6 proxy is disable")
            return
        }
        val tcpHeader = TcpHeader(ipHeader, packet.packet, ipHeader.headerLength)
        val sourcePort = tcpHeader.sourcePort
        if (!ports.contains(sourcePort)) {
            // 来源不是代理服务器，说明该数据包是被代理客户端发出来的请求包
            if (WireBare.dynamicConfig.reqBandwidthLimiter.max <= 0L) {
                transmit(packet, ipHeader, tcpHeader, outputStream)
            } else {
                reqPacketQueue.put(PendingPacket(packet, ipHeader, tcpHeader, outputStream))
                if (!waitingReqTransmit.get()) {
                    lock.withLock {
                        condition.signal()
                    }
                }
            }
        } else {
            if (WireBare.dynamicConfig.rspBandwidthLimiter.max <= 0L) {
                transmit(packet, ipHeader, tcpHeader, outputStream)
            } else {
                rspPacketQueue.put(PendingPacket(packet, ipHeader, tcpHeader, outputStream))
                if (!waitingRspTransmit.get()) {
                    lock.withLock {
                        condition.signal()
                    }
                }
            }
        }
    }

    private fun transmit(
        packet: Packet,
        ipHeader: IPHeader,
        tcpHeader: TcpHeader,
        outputStream: OutputStream
    ) {
        bandwidthStat.onPacketTransmit(packet.length)
        // 来源地址和端口
        val sourceAddress = ipHeader.sourceAddress
        val sourcePort = tcpHeader.sourcePort

        // 目的地址和端口
        val destinationAddress = ipHeader.destinationAddress
        val destinationPort = tcpHeader.destinationPort

        if (!ports.contains(sourcePort)) {
            // 来源不是代理服务器，说明该数据包是被代理客户端发出来的请求包
            reqBandwidthStat.onPacketTransmit(packet.length)
            sessionStore.insert(
                sourcePort, destinationAddress, destinationPort
            )

            // 根据端口号分配给固定的服务器
            val proxyServerPort = servers[
                sourcePort.port.convertPortToInt % servers.size
            ].proxyServerPort

            // 将被代理客户端的请求数据包转发给代理服务器
            ipHeader.sourceAddress = destinationAddress

            when (ipHeader.ipVersion) {
                IPVersion.IPv4 -> ipHeader.destinationAddress = tunIPv4Address
                IPVersion.IPv6 -> ipHeader.destinationAddress = tunIPv6Address
            }
            tcpHeader.destinationPort = proxyServerPort

            WireBareLogger.debug(
                TAG,
                "[${ipHeader.ipVersion.name}-TCP] client $sourcePort > proxy client $proxyServerPort " +
                        "seq = ${tcpHeader.sequenceNumber.toUInt()} " +
                        "ack = ${tcpHeader.acknowledgmentNumber.toUInt()} " +
                        "flag = 0b${tcpHeader.flag.toUByte().toString(2).padStart(6, '0')} " +
                        "length = ${tcpHeader.dataLength}"
            )
        } else {
            // 来源是代理服务器，说明该数据包是响应包
            rspBandwidthStat.onPacketTransmit(packet.length)
            val session = sessionStore.query(
                destinationPort
            ) ?: throw IllegalStateException(
                "cannot find session for response(port = $destinationPort)"
            )

//            if (tcpHeader.fin) {
//                session.tryDrop()
//            }

            // 将远程服务器的响应包转发给被代理客户端
            ipHeader.sourceAddress = destinationAddress
            tcpHeader.sourcePort = session.destinationPort

            when (ipHeader.ipVersion) {
                IPVersion.IPv4 -> ipHeader.destinationAddress = tunIPv4Address
                IPVersion.IPv6 -> ipHeader.destinationAddress = tunIPv6Address
            }

            WireBareLogger.debug(
                TAG,
                "[${ipHeader.ipVersion.name}-TCP] client $destinationPort < proxy client $sourcePort " +
                        "seq = ${tcpHeader.sequenceNumber.toUInt()} " +
                        "ack = ${tcpHeader.acknowledgmentNumber.toUInt()} " +
                        "flag = 0b${tcpHeader.flag.toUByte().toString(2).padStart(6, '0')} " +
                        "length = ${tcpHeader.dataLength}"
            )
        }

        ipHeader.notifyCheckSum()
        tcpHeader.notifyCheckSum()

        outputStream.write(packet.packet, 0, packet.length)
    }

}