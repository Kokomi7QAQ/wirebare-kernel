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

import top.sankokomi.wirebare.kernel.net.IpVersion
import top.sankokomi.wirebare.kernel.ssl.JKS
import top.sankokomi.wirebare.kernel.util.convertIpv4ToInt
import top.sankokomi.wirebare.kernel.util.convertIpv6ToInt
import java.security.KeyStore
import java.security.cert.X509Certificate
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

object WireBareHelper {

    /**
     * 检查指定的证书是否被系统信任
     * */
    fun checkSystemTrustCert(jks: JKS): Boolean {
        try {
            val keyStore: KeyStore = KeyStore.getInstance(jks.type).also {
                it.load(jks.jksStream(), jks.password)
            }
            val certificate = keyStore.getCertificate(jks.alias) as X509Certificate
            val tmf = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm()
            )
            tmf.init(null as KeyStore?)
            val trustManagers = tmf.trustManagers
            if (trustManagers.size != 1 || trustManagers[0] !is X509TrustManager) {
                return false
            }
            val tm = trustManagers[0] as X509TrustManager
            tm.checkClientTrusted(arrayOf(certificate), jks.algorithm)
            return true
        } catch (_: Exception) {
            return false
        }
    }

    /**
     * Parse ip version from [ipAddress].
     *
     * @return the version of ip, or null is illegal.
     * */
    fun parseIpVersion(ipAddress: String?): IpVersion? {
        ipAddress ?: return null
        try {
            ipAddress.convertIpv6ToInt
            return IpVersion.IPv6
        } catch (_: Exception) {
        }
        try {
            ipAddress.convertIpv4ToInt
            return IpVersion.IPv4
        } catch (_: Exception) {
        }
        return null
    }

}