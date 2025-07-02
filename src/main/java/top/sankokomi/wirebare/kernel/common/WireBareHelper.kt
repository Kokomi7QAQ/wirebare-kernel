package top.sankokomi.wirebare.kernel.common

import java.security.KeyStore

object WireBareHelper {

    /**
     * 检查指定的证书 [certName] 是否被系统信任
     *
     * @param certName 证书的文件名，一般是 8位小写字母/数字.0 例如: 3c899c73.0
     * */
    fun checkSystemTrustCert(certName: String): Boolean {
        runCatching {
            val targetCert = "system:$certName"
            val keyStore = KeyStore.getInstance("AndroidCAStore")
            keyStore.load(null, null)
            val aliases = keyStore.aliases()
            while (aliases.hasMoreElements()) {
                val alias = aliases.nextElement() ?: continue
                if (alias == targetCert) {
                    return true
                }
            }
        }
        return false
    }

}