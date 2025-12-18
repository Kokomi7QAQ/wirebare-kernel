package top.sankokomi.wirebare.kernel.net

import java.math.BigInteger

interface IIpHeader {

    /**
     * 数据段长度
     * */
    val dataLength: Int

    /**
     * 整个 IP 头的长度
     * */
    val headerLength: Int

    /**
     * 地址部分的校验和
     * */
    val addressSum: BigInteger

    /**
     * 协议版本号
     * */
    val protocol: Byte

    var sourceAddress: IpAddress

    var destinationAddress: IpAddress
}