package top.sankokomi.wirebare.kernel.net

import java.math.BigInteger

interface IIpHeader {
    val dataLength: Int
    val addressSum: BigInteger
    val protocol: Byte
}