package top.sankokomi.wirebare.kernel.service

import android.net.VpnService
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import top.sankokomi.wirebare.kernel.common.WireBare
import top.sankokomi.wirebare.kernel.common.WireBareConfiguration
import top.sankokomi.wirebare.kernel.net.IIpHeader
import top.sankokomi.wirebare.kernel.net.IpHeader
import top.sankokomi.wirebare.kernel.net.Ipv4Header
import top.sankokomi.wirebare.kernel.net.Ipv6Header
import top.sankokomi.wirebare.kernel.net.Packet
import top.sankokomi.wirebare.kernel.net.Protocol
import top.sankokomi.wirebare.kernel.tcp.TcpPacketInterceptor
import top.sankokomi.wirebare.kernel.udp.UdpPacketInterceptor
import top.sankokomi.wirebare.kernel.util.WireBareLogger
import top.sankokomi.wirebare.kernel.util.closeSafely
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InterruptedIOException
import kotlin.random.Random

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
    private var buffer = ByteArray(configuration.mtu)

    private fun dispatch() {
        if (!isActive) return
        // 启动协程接收 TUN 虚拟网卡的输入流
        launch(Dispatchers.IO) {
            try {
                while (isActive) {
                    var length = 0
                    while (isActive) {
                        length = 0
                        try {
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

                    val mockPacketLossProbability =
                        WireBare.dynamicConfiguration.mockPacketLossProbability
                    if (mockPacketLossProbability == 100) {
                        WireBareLogger.info("模拟丢包 全丢!")
                        continue
                    } else if (
                        mockPacketLossProbability in 1..100
                    ) {
                        when ((Random.nextInt() % 100) + 1) {
                            in 1..mockPacketLossProbability -> {
                                WireBareLogger.info("模拟丢包 丢!")
                                continue
                            }
                        }
                    }

                    val packet = Packet(buffer, length)

                    val ipHeader: IIpHeader
                    when (val ipVersion = IpHeader.readIpVersion(packet, 0)) {
                        IpHeader.VERSION_4 -> {
                            if (packet.length < Ipv4Header.MIN_IPV4_LENGTH) {
                                WireBareLogger.warn("报文长度小于 ${Ipv4Header.MIN_IPV4_LENGTH}")
                                continue
                            }
                            ipHeader = Ipv4Header(packet.packet, 0)
                        }

                        IpHeader.VERSION_6 -> {
                            if (packet.length < Ipv6Header.IPV6_STANDARD_LENGTH) {
                                WireBareLogger.warn("报文长度小于 ${Ipv6Header.IPV6_STANDARD_LENGTH}")
                                continue
                            }
                            ipHeader = Ipv6Header(packet.packet, 0)
                        }

                        else -> {
                            WireBareLogger.debug("未知的 ip 版本号 0b${ipVersion.toString(2)}")
                            continue
                        }
                    }

                    val interceptor = interceptors[Protocol.parse(ipHeader.protocol)]
                    if (interceptor == null) {
                        WireBareLogger.warn("未知的协议代号 0b${ipHeader.protocol.toString(2)}")
                        continue
                    }

                    try {
                        // 拦截器拦截输入流
                        when (ipHeader) {
                            is Ipv4Header -> {
                                interceptor.intercept(ipHeader, packet, outputStream)
                            }

                            is Ipv6Header -> {
                                if (configuration.enableIpv6) {
                                    interceptor.intercept(ipHeader, packet, outputStream)
                                }
                            }
                        }
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