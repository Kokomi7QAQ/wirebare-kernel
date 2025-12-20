package top.sankokomi.wirebare.kernel.interceptor.tcp

import top.sankokomi.wirebare.kernel.common.WireBareConfiguration
import top.sankokomi.wirebare.kernel.interceptor.http.HttpTcpInterceptor
import top.sankokomi.wirebare.kernel.net.TcpSession
import java.nio.ByteBuffer

/**
 * Tcp 虚拟网关
 * */
class TcpVirtualGateway(
    configuration: WireBareConfiguration
) {

    private val interceptorChain: TcpInterceptChain

    init {
        val interceptors = mutableListOf<TcpInterceptor>()
        interceptors.add(HttpTcpInterceptor(configuration))
        interceptorChain = TcpInterceptChain(interceptors)
    }

    fun onRequest(
        buffer: ByteBuffer,
        session: TcpSession,
        tunnel: TcpTunnel
    ) {
        interceptorChain.processRequestFirst(buffer, session, tunnel)
    }

    fun onRequestFinished(
        session: TcpSession,
        tunnel: TcpTunnel
    ) {
        interceptorChain.processRequestFinishedFirst(session, tunnel)
    }

    fun onResponse(
        buffer: ByteBuffer,
        session: TcpSession,
        tunnel: TcpTunnel
    ) {
        interceptorChain.processResponseFirst(buffer, session, tunnel)
    }

    fun onResponseFinished(
        session: TcpSession,
        tunnel: TcpTunnel
    ) {
        interceptorChain.processResponseFinishedFirst(session, tunnel)
    }

}