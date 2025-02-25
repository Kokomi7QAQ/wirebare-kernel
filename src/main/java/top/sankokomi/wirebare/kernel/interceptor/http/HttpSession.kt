package top.sankokomi.wirebare.kernel.interceptor.http

import top.sankokomi.wirebare.kernel.net.TcpSession

data class HttpSession(
    val request: HttpRequest,
    val response: HttpResponse,
    val tcpSession: TcpSession
)