package top.sankokomi.wirebare.kernel.ssl

import top.sankokomi.wirebare.kernel.net.TcpSession
import java.nio.ByteBuffer

class ResponseSSLCodec(
    private val engineFactory: SSLEngineFactory
) : SSLCodec() {

    private val clientSSLEngineMap = hashMapOf<TcpSession, WireBareSSLEngine>()

    override fun createSSLEngineWrapper(session: TcpSession, host: String): WireBareSSLEngine? {
        return clientSSLEngineMap[session] ?: let {
            engineFactory.createClientSSLEngine(host, session.destinationPort)?.also {
                clientSSLEngineMap[session] = it
                it.name = "RSP-$host:${session.destinationPort}"
            }
        }
    }

    internal fun handshakeIfNecessary(
        session: TcpSession,
        host: String,
        callback: SSLCallback
    ) {
        createSSLEngineWrapper(session, host)?.handshake(
            ByteBuffer.allocate(0),
            callback
        )
    }
}