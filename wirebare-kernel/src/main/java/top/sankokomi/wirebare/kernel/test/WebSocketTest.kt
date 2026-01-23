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
import top.sankokomi.wirebare.kernel.util.WireBareLogger
import java.util.concurrent.TimeUnit

@Test
fun testWebSocket(duration: Long = 60000L) {
    val manager = WebSocketCommunicationManager()
    manager.startCommunication()
    CoroutineScope(Dispatchers.IO).launch {
        delay(duration)
        manager.stopCommunication()
        log("communication end")
    }
}

class WebSocketCommunicationManager {
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

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

private fun log(s: Any?) {
    WireBareLogger.test("WebSocketTest", s)
}