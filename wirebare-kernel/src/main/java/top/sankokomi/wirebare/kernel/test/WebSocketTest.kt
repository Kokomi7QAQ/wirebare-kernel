package top.sankokomi.wirebare.kernel.test

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import top.sankokomi.wirebare.kernel.annotation.Test
import top.sankokomi.wirebare.kernel.common.WireBare
import top.sankokomi.wirebare.kernel.util.WireBareLogger
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.TimeUnit
import javax.net.SocketFactory

@Test
fun testWebSocket(duration: Long = 60000L, protected: Boolean) {
    val manager = WebSocketCommunicationManager(protected)
    manager.startCommunication()
    CoroutineScope(Dispatchers.IO).launch {
        delay(duration)
        manager.stopCommunication()
        log("communication end")
    }
}

class WebSocketCommunicationManager(protected: Boolean) {
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .socketFactory(
            if (protected) ProtectedSocketFactory() else SocketFactory.getDefault()
        ).build()

    private var clientWebSocket: WebSocket? = null

    private var timerJob: Job? = null

    private val webSocketListener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            log("client opened")
            startTimer()
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            log("client receive: $text")
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            log("connection closed")
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            log("connection failure: ${t.message}")
        }
    }

    fun startCommunication() {
        val request = Request.Builder()
            .url("ws://echo.websocket.org")
            .build()

        clientWebSocket = client.newWebSocket(request, webSocketListener)
    }

    private fun startTimer() {
        timerJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                delay(5000)
                val msg = "hello ${System.currentTimeMillis()}"
                clientWebSocket?.send(msg)
                log("client send: $msg")
            }
        }
    }

    fun stopCommunication() {
        timerJob?.cancel()
        clientWebSocket?.close(1000, null)
        client.dispatcher.executorService.shutdown()
    }
}

private class ProtectedSocketFactory : SocketFactory() {

    override fun createSocket(): Socket? {
        return Socket().protect()
    }

    override fun createSocket(
        host: String?,
        port: Int
    ): Socket? {
        return Socket(host, port).protect()
    }

    override fun createSocket(
        host: InetAddress?,
        port: Int
    ): Socket? {
        return Socket(host, port).protect()
    }

    override fun createSocket(
        host: String?,
        port: Int,
        localHost: InetAddress?,
        localPort: Int
    ): Socket? {
        return Socket(host, port, localHost, localPort).protect()
    }

    override fun createSocket(
        address: InetAddress?,
        port: Int,
        localAddress: InetAddress?,
        localPort: Int
    ): Socket? {
        return Socket(address, port, localAddress, localPort).protect()
    }

    private fun Socket.protect(): Socket {
        val socket = this
        // must bind before we protect it
        socket.bind(InetSocketAddress(0))
        WireBare protect socket
        return this
    }
}

private fun log(s: Any?) {
    WireBareLogger.test("WebSocketTest", s)
}