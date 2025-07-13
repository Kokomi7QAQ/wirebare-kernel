package top.sankokomi.wirebare.kernel.service

import android.app.Notification
import android.content.Intent
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
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

abstract class WireBareProxyService : VpnService(),
    CoroutineScope by CoroutineScope(SupervisorJob() + Dispatchers.IO) {

    companion object {
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

    final override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        WireBareLogger.info("Service startCommand")
        intent ?: return START_NOT_STICKY
        when (intent.action) {
            WIREBARE_ACTION_PROXY_VPN_START -> startWireBare()
            WIREBARE_ACTION_PROXY_VPN_STOP -> stopWireBare()
            else -> throw IllegalArgumentException("意料之外的 Action")
        }
        return super.onStartCommand(intent, flags, startId)
    }

    @Volatile
    private var fd: CompletableDeferred<ParcelFileDescriptor?> = CompletableDeferred()

    private fun startWireBare() {
        WireBareLogger.info("Service startWireBare")
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
        WireBareLogger.info("Service stopWireBare")
        launch(Dispatchers.IO) {
            fd.await().closeSafely()
            cancel()
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        WireBareLogger.info("Service destroy")
        super.onDestroy()
        WireBare.notifyVpnStatusChanged(ProxyStatus.DEAD)
    }
}