package top.sankokomi.wirebare.kernel.interceptor.http.async

interface AsyncHttpInterceptorFactory {
    fun create(): AsyncHttpInterceptor
}