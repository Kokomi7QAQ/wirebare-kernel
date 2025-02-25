package top.sankokomi.wirebare.kernel.interceptor.http

interface HttpInterceptorFactory {
    fun create(): HttpInterceptor
}