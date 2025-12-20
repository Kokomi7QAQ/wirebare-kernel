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

package top.sankokomi.wirebare.kernel.net

import top.sankokomi.wirebare.kernel.util.convertIpv4ToInt
import top.sankokomi.wirebare.kernel.util.convertIpv4ToString
import top.sankokomi.wirebare.kernel.util.convertIpv6ToInt
import top.sankokomi.wirebare.kernel.util.convertIpv6ToString

/**
 * ip 地址
 * */
class IpAddress {

    val ipVersion: IpVersion

    val intIpv4: Int

    val intIpv6: IntIpv6

    val stringIp: String

    constructor(ipv4Address: Int) {
        this.ipVersion = IpVersion.IPv4
        this.intIpv4 = ipv4Address
        this.stringIp = intIpv4.convertIpv4ToString
        this.intIpv6 = IntIpv6(0L, 0L)
    }

    constructor(ipv6Address: IntIpv6) {
        this.ipVersion = IpVersion.IPv6
        this.intIpv6 = ipv6Address
        this.stringIp = intIpv6.convertIpv6ToString
        this.intIpv4 = 0
    }

    constructor(address: String, ipVersion: IpVersion) {
        this.ipVersion = ipVersion
        when (ipVersion) {
            IpVersion.IPv4 -> {
                this.intIpv4 = address.convertIpv4ToInt
                this.stringIp = address
                this.intIpv6 = IntIpv6(0L, 0L)
            }

            IpVersion.IPv6 -> {
                this.intIpv6 = address.convertIpv6ToInt
                this.stringIp = address
                this.intIpv4 = 0
            }
        }
    }

    override fun toString(): String = stringIp

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as IpAddress
        return stringIp == other.stringIp
    }

    override fun hashCode(): Int {
        return stringIp.hashCode()
    }

}