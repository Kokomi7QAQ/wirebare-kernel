/*
 * MIT License
 *
 * Copyright (c) 2025 KokomiQAQ
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package top.sankokomi.wirebare.kernel.common

import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Handler
import android.os.Looper
import androidx.annotation.MainThread
import androidx.core.content.ContextCompat
import top.sankokomi.wirebare.kernel.common.WireBare.addImportantEventListener
import top.sankokomi.wirebare.kernel.common.WireBare.addVpnProxyStatusListener
import top.sankokomi.wirebare.kernel.common.WireBare.removeImportantEventListener
import top.sankokomi.wirebare.kernel.common.WireBare.removeVpnProxyStatusListener
import top.sankokomi.wirebare.kernel.common.WireBare.startProxy
import top.sankokomi.wirebare.kernel.common.WireBare.stopProxy
import top.sankokomi.wirebare.kernel.service.WireBareProxyService
import top.sankokomi.wirebare.kernel.util.LogLevel
import top.sankokomi.wirebare.kernel.util.WireBareLogger
import java.net.DatagramSocket
import java.net.Socket

object WireBare {

    private lateinit var appContext: Context

    private var vpnService: WireBareProxyService? = null

    private var _configuration: WireBareConfiguration? = null

    var dynamicConfig: DynamicConfiguration = DynamicConfiguration()

    private val listeners: MutableSet<IProxyStatusListener> = hashSetOf()

    private val eventListeners: MutableSet<IImportantEventListener> = hashSetOf()

    /**
     * [WireBareProxyService] 的实时状态
     * */
    var proxyStatus: ProxyStatus = ProxyStatus.DEAD
        private set

    /**
     * 启动代理服务
     *
     * @param configuration WireBare 的配置
     *
     * @see [WireBareConfiguration]
     * @see [stopProxy]
     * */
    @MainThread
    fun startProxy(configuration: WireBareConfiguration.() -> Unit) {
        if (proxyStatus == ProxyStatus.ACTIVE) return
        notifyVpnStatusChanged(ProxyStatus.STARTING)
        _configuration = WireBareConfiguration()
            .apply(configuration)
        val intent = Intent(WireBareProxyService.WIREBARE_ACTION_PROXY_VPN_START).apply {
            `package` = appContext.packageName
        }
        ContextCompat.startForegroundService(appContext, intent)
    }

    /**
     * 结束代理服务
     *
     * @see [startProxy]
     * */
    @MainThread
    fun stopProxy() {
        if (proxyStatus == ProxyStatus.DEAD) return
        notifyVpnStatusChanged(ProxyStatus.DYING)
        val intent = Intent(WireBareProxyService.WIREBARE_ACTION_PROXY_VPN_STOP).apply {
            `package` = appContext.packageName
        }
        appContext.startService(intent)
    }

    /**
     * 注册代理服务状态监听器，需要进行注销
     *
     * @see [IProxyStatusListener]
     * @see [removeVpnProxyStatusListener]
     * */
    @MainThread
    fun addVpnProxyStatusListener(listener: IProxyStatusListener) {
        listener.onVpnStatusChanged(ProxyStatus.DEAD, proxyStatus)
        listeners.add(listener)
    }

    /**
     * 注销代理服务状态监听器
     *
     * @see [IProxyStatusListener]
     * @see [addVpnProxyStatusListener]
     * */
    @MainThread
    fun removeVpnProxyStatusListener(listener: IProxyStatusListener): Boolean {
        return listeners.remove(listener)
    }

    /**
     * 注册重要事件监听器
     *
     * @see [IImportantEventListener]
     * @see [EventSynopsis]
     * @see [removeImportantEventListener]
     * */
    @MainThread
    fun addImportantEventListener(listener: IImportantEventListener) {
        eventListeners.add(listener)
    }

    /**
     * 注销代理服务状态监听器
     *
     * @see [IImportantEventListener]
     * @see [EventSynopsis]
     * @see [addImportantEventListener]
     * */
    @MainThread
    fun removeImportantEventListener(listener: IImportantEventListener): Boolean {
        return eventListeners.remove(listener)
    }

    /**
     * 配置日志等级
     *
     * @see [LogLevel]
     * */
    @LogLevel
    var logLevel: Int
        get() = WireBareLogger.LOG_LEVEL
        set(level) {
            WireBareLogger.LOG_LEVEL = level
        }

    /**
     * This function can only be called when proxy service is running.
     *
     * @see [VpnService.protect]
     * */
    infix fun protect(socket: Int): Boolean {
        return vpnService?.protect(socket) ?: false
    }

    /**
     * This function can only be called when proxy service is running.
     *
     * @see [VpnService.protect]
     * */
    infix fun protect(socket: Socket): Boolean {
        return vpnService?.protect(socket) ?: false
    }

    /**
     * This function can only be called when proxy service is running.
     *
     * @see [VpnService.protect]
     * */
    infix fun protect(socket: DatagramSocket): Boolean {
        return vpnService?.protect(socket) ?: false
    }

    internal infix fun attach(context: Context) {
        appContext = context
    }

    internal infix fun attach(service: WireBareProxyService) {
        vpnService = service
    }

    internal infix fun detach(service: WireBareProxyService) {
        if (vpnService == service) {
            vpnService = null
        }
    }

    internal fun notifyVpnStatusChanged(newStatus: ProxyStatus) {
        Handler(Looper.getMainLooper()).post {
            WireBareLogger.info("statusChange: old = $proxyStatus, new = $newStatus")
            if (newStatus == proxyStatus) return@post
            val oldStatus = proxyStatus
            proxyStatus = newStatus
            listeners.removeAll { listener ->
                listener.onVpnStatusChanged(oldStatus, newStatus)
            }
        }
    }

    internal fun postImportantEvent(event: ImportantEvent) {
        eventListeners.forEach { listener ->
            listener.onPost(event)
        }
    }

    internal val configuration: WireBareConfiguration
        get() {
            val config = _configuration
            if (config != null) return config
            throw NullPointerException("WireBare 配置为空")
        }

}