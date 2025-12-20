package top.sankokomi.wirebare.kernel.interceptor.tcp

import java.nio.ByteBuffer

interface TcpTunnel {
    /**
     * 将 [buffer] 写入到远端服务器
     * */
    fun writeToRemoteServer(buffer: ByteBuffer)

    /**
     * 将 [buffer] 写入到被代理客户端
     * */
    fun writeToLocalClient(buffer: ByteBuffer)
}