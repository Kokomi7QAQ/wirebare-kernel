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

import android.os.Build
import android.os.ParcelFileDescriptor
import android.system.OsConstants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.isActive
import top.sankokomi.wirebare.kernel.common.WireBareConfiguration
import top.sankokomi.wirebare.kernel.service.PacketDispatcher.Companion.dispatchWith

/**
 * VPN 代理服务的启动器
 * */
internal class ProxyLauncher private constructor(
    internal val configuration: WireBareConfiguration,
    internal val proxyService: WireBareProxyService
) : CoroutineScope by proxyService {

    companion object {
        internal infix fun WireBareProxyService.launchWith(
            configuration: WireBareConfiguration
        ): ParcelFileDescriptor? {
            return ProxyLauncher(configuration, this).launch()
        }
    }

    private fun launch(): ParcelFileDescriptor? {
        if (!isActive) return null
        // 配置 VPN 服务
        val builder = proxyService.Builder().also { builder ->
            with(configuration) {
                builder.setMtu(mtu)
                    .addAddress(ipv4Address, ipv4PrefixLength)
                    .allowFamily(OsConstants.AF_INET)
                    .setBlocking(true)
                if (enableIPv6) {
                    builder.addAddress(ipv6Address, ipv6PrefixLength)
                        .allowFamily(OsConstants.AF_INET6)
                }
                for (route in routes) {
                    builder.addRoute(route.first, route.second)
                }
                for (dns in dnsServers) {
                    builder.addDnsServer(dns)
                }
                // 允许和不允许应用只能配置其中一个
                for (application in allowedApplications) {
                    builder.addAllowedApplication(application)
                }
                if (allowedApplications.isNotEmpty()) {
                    builder.addAllowedApplication(proxyService.packageName)
                }
                if (disallowedApplications.contains(proxyService.packageName)) {
                    throw IllegalArgumentException("母应用必须接入到代理服务中")
                }
                for (application in disallowedApplications) {
                    builder.addDisallowedApplication(application)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    builder.setMetered(false)
                }
            }
        }

        return this dispatchWith builder
    }

}