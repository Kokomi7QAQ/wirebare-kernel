package top.sankokomi.wirebare.kernel.udp

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import top.sankokomi.wirebare.kernel.common.WireBareConfiguration
import top.sankokomi.wirebare.kernel.net.Ipv4Header
import top.sankokomi.wirebare.kernel.net.Port
import top.sankokomi.wirebare.kernel.net.UdpHeader
import top.sankokomi.wirebare.kernel.net.UdpSessionStore
import top.sankokomi.wirebare.kernel.proxy.NioProxyServer
import top.sankokomi.wirebare.kernel.service.WireBareProxyService
import top.sankokomi.wirebare.kernel.util.WireBareLogger
import top.sankokomi.wirebare.kernel.util.closeSafely
import top.sankokomi.wirebare.kernel.util.convertPortToInt
import java.io.OutputStream
import java.nio.channels.DatagramChannel
import java.nio.channels.Selector

/**
 * UDP 代理服务器
 *
 * 负责启动 [UdpRealTunnel] 并让 [UdpRealTunnel] 与远程服务器通信
 * */
internal class UdpProxyServer(
    private val sessionStore: UdpSessionStore,
    private val configuration: WireBareConfiguration,
    private val proxyService: WireBareProxyService
) : NioProxyServer(), CoroutineScope by proxyService {

    override val selector: Selector = Selector.open()

    private val tunnels = hashMapOf<Port, UdpRealTunnel>()

    /**
     * 开始代理 UDP 数据包
     * */
    internal fun proxy(
        ipv4Header: Ipv4Header,
        udpHeader: UdpHeader,
        outputStream: OutputStream
    ) {
        launch(Dispatchers.IO) {
            val sourcePort = udpHeader.sourcePort
            try {
                val tunnel =
                    tunnels[sourcePort] ?: createTunnel(ipv4Header, udpHeader, outputStream)
                tunnel.write(udpHeader.data)
            } catch (e: Exception) {
                tunnels.remove(sourcePort)?.closeSafely()
                WireBareLogger.error(e)
            }
        }
    }

    /**
     * 创建一个 [UdpRealTunnel]
     * */
    private fun createTunnel(
        ipv4Header: Ipv4Header,
        udpHeader: UdpHeader,
        outputStream: OutputStream
    ): UdpRealTunnel {
        val session = sessionStore.query(
            udpHeader.sourcePort
        ) ?: throw IllegalStateException("一个 UDP 请求因为找不到指定会话而代理失败")

        return UdpRealTunnel(
            DatagramChannel.open(),
            selector,
            outputStream,
            session,
            udpHeader,
            configuration,
            proxyService
        ).also {
            it.connectRemoteServer(
                ipv4Header.destinationAddress.stringIp,
                udpHeader.destinationPort.port.convertPortToInt
            )
            tunnels[session.sourcePort] = it
        }
    }

    override fun release() {
        for (tunnel in tunnels.values) {
            tunnel.closeSafely()
        }
        tunnels.clear()
    }

}