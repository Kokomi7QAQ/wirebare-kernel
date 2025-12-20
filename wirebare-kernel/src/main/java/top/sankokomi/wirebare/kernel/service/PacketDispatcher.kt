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

package top.sankokomi.wirebare.kernel.service

import android.net.VpnService
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import top.sankokomi.wirebare.kernel.common.WireBareConfiguration
import top.sankokomi.wirebare.kernel.net.IPHeader
import top.sankokomi.wirebare.kernel.net.Packet
import top.sankokomi.wirebare.kernel.net.Protocol
import top.sankokomi.wirebare.kernel.tcp.TcpPacketInterceptor
import top.sankokomi.wirebare.kernel.udp.UdpPacketInterceptor
import top.sankokomi.wirebare.kernel.util.WireBareLogger
import top.sankokomi.wirebare.kernel.util.closeSafely
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InterruptedIOException

/**
 * ip 包调度者，负责从代理服务的输入流中获取 ip 包并根据 ip 头的信息分配给对应的 [PacketInterceptor]
 * */
internal class PacketDispatcher private constructor(
    private val configuration: WireBareConfiguration,
    private val proxyDescriptor: ParcelFileDescriptor,
    private val proxyService: WireBareProxyService
) : CoroutineScope by proxyService {

    companion object {
        internal infix fun ProxyLauncher.dispatchWith(builder: VpnService.Builder): ParcelFileDescriptor {
            val proxyDescriptor = builder.establish() ?: throw IllegalStateException(
                "请先准备代理服务"
            )
            PacketDispatcher(configuration, proxyDescriptor, proxyService).dispatch()
            return proxyDescriptor
        }
    }

    /**
     * ip 包拦截器
     * */
    private val interceptors = hashMapOf<Protocol, PacketInterceptor>(
        Protocol.TCP to TcpPacketInterceptor(configuration, proxyService),
        Protocol.UDP to UdpPacketInterceptor(configuration, proxyService)
    )

    /**
     * 代理服务输入流
     * */
    private val inputStream = FileInputStream(proxyDescriptor.fileDescriptor)

    /**
     * 代理服务输出流
     * */
    private val outputStream = FileOutputStream(proxyDescriptor.fileDescriptor)

    /**
     * 缓冲流
     * */
    private var buffer = ByteArray(1)

    private fun dispatch() {
        if (!isActive) return
        // 启动协程接收 TUN 虚拟网卡的输入流
        launch(Dispatchers.IO) {
            try {
                while (isActive) {
                    var length = 0
                    while (isActive) {
                        try {
                            buffer = ByteArray(configuration.mtu)
                            // 从 VPN 服务中读取输入流
                            length = inputStream.read(buffer)
                        } catch (e: Exception) {
                            if (e !is InterruptedIOException) {
                                WireBareLogger.error(e)
                            }
                            return@launch
                        }
                        if (length != 0) {
                            break
                        }
                    }

                    if (length <= 0) continue

                    val packet = Packet(buffer, length)

                    val ipHeader: IPHeader = IPHeader.parse(packet.packet, packet.length, 0) ?: continue

                    val interceptor = interceptors[Protocol.parse(ipHeader.dataProtocol)]
                    if (interceptor == null) {
                        WireBareLogger.warn("未知的协议代号 0b${ipHeader.dataProtocol.toString(2)}")
                        continue
                    }

                    try {
                        // 拦截器拦截输入流
                        interceptor.intercept(ipHeader, packet, outputStream)
                    } catch (e: Exception) {
                        WireBareLogger.error(e)
                    }
                }
            } catch (e: Exception) {
                WireBareLogger.error(e)
            }
            // 关闭所有资源
            closeSafely(proxyDescriptor, inputStream, outputStream)
        }
    }

}