package top.sankokomi.wirebare.kernel.net

internal interface Ipv6ExtHeaderLengthResolver {
    fun nextHeaderToResolve(): Int
    fun resolveLength(packet: ByteArray, offset: Int): Int
}