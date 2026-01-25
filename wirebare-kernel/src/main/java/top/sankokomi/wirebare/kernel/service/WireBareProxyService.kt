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

package top.sankokomi.wirebare.kernel.service

import android.app.Notification
import android.content.Intent
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.ServiceCompat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import top.sankokomi.wirebare.kernel.common.ProxyStatus
import top.sankokomi.wirebare.kernel.common.WireBare
import top.sankokomi.wirebare.kernel.service.ProxyLauncher.Companion.launchWith
import top.sankokomi.wirebare.kernel.util.WireBareLogger
import top.sankokomi.wirebare.kernel.util.closeSafely
import top.sankokomi.wirebare.kernel.util.defaultNotification

@Suppress("VpnServicePolicy")
abstract class WireBareProxyService : VpnService(),
    CoroutineScope by CoroutineScope(SupervisorJob() + Dispatchers.IO) {

    companion object {
        private const val TAG = "WireBareProxyService"

        internal const val WIREBARE_ACTION_PROXY_VPN_START =
            "top.sankokomi.wirebare.core.action.Start"

        internal const val WIREBARE_ACTION_PROXY_VPN_STOP =
            "top.sankokomi.wirebare.core.action.Stop"
    }

    /**
     * 通知通道 ID ，默认 WireBareProxyService
     * */
    protected open var channelId: String = "WireBareProxyService"

    /**
     * 通知 ID ，默认 222
     * */
    protected open var notificationId: Int = 222

    /**
     * 创建通知，默认 [VpnService.defaultNotification]
     *
     * 代理抓包对于用户来说有危险性，因此前台服务并显示通知来通知用户网络正在被代理是必须的
     *
     * 其次需要前台服务来保证服务的稳定，避免太容易因为系统资源不足而导致销毁
     * */
    protected open var notification: WireBareProxyService.() -> Notification =
        { defaultNotification(channelId) }

    override fun onCreate() {
        super.onCreate()
        WireBare attach this
    }

    final override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        WireBareLogger.info(TAG, "service startCommand")
        intent ?: return START_NOT_STICKY
        when (intent.action) {
            WIREBARE_ACTION_PROXY_VPN_START -> startWireBare()
            WIREBARE_ACTION_PROXY_VPN_STOP -> stopWireBare()
            else -> throw IllegalArgumentException("unexpected action")
        }
        return super.onStartCommand(intent, flags, startId)
    }

    @Volatile
    private var fd: CompletableDeferred<ParcelFileDescriptor?> = CompletableDeferred()

    private fun startWireBare() {
        WireBareLogger.info(TAG, "service startWireBare")
        WireBare.notifyVpnStatusChanged(ProxyStatus.ACTIVE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(notificationId, notification(), FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(notificationId, notification())
        }
        val configuration = WireBare.configuration.copy()
        launch(Dispatchers.IO) {
            try {
                fd.complete(this@WireBareProxyService launchWith configuration)
            } catch (_: Exception) {
                fd.complete(null)
            }
        }
    }

    private fun stopWireBare() {
        WireBareLogger.info(TAG, "service stopWireBare")
        launch(Dispatchers.IO) {
            fd.await().closeSafely()
            this@WireBareProxyService.cancel()
        }
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        WireBareLogger.info(TAG, "service destroy")
        super.onDestroy()
        WireBare.notifyVpnStatusChanged(ProxyStatus.DEAD)
        WireBare detach this
    }
}