package top.sankokomi.wirebare.kernel.ssl

import java.io.InputStream

class JKS(
    val jksStream: () -> InputStream,
    val alias: String,
    val password: CharArray,
    val algorithm: String = "RSA",
    val type: String = "PKCS12",
    val organization: String = "WB",
    val organizationUnit: String = "WB"
)