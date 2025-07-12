package top.sankokomi.wirebare.kernel.common

import top.sankokomi.wirebare.kernel.ssl.JKS
import java.security.KeyStore
import java.security.cert.X509Certificate
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

object WireBareHelper {

    /**
     * 检查指定的证书是否被系统信任
     * */
    fun checkSystemTrustCert(jks: JKS): Boolean {
        runCatching {
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
        }
        return false
    }

}