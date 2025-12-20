package top.sankokomi.wirebare.kernel.net

import top.sankokomi.wirebare.kernel.util.convertPortToString

/**
 * 端口号
 * */
data class Port(
    internal val port: Short
) {

    override fun toString(): String {
        return port.convertPortToString
    }

}