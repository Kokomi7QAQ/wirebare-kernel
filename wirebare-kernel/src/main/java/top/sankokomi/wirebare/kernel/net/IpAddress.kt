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

import top.sankokomi.wirebare.kernel.util.convertIPv4ToInt
import top.sankokomi.wirebare.kernel.util.convertIPv4ToString
import top.sankokomi.wirebare.kernel.util.convertIPv6ToInt
import top.sankokomi.wirebare.kernel.util.convertIPv6ToString

/**
 * ip 地址
 * */
class IpAddress {

    val ipVersion: IPVersion

    val intIPv4: Int

    val intIPv6: IntIPv6

    val stringIP: String

    constructor(ipv4Address: Int) {
        this.ipVersion = IPVersion.IPv4
        this.intIPv4 = ipv4Address
        this.stringIP = intIPv4.convertIPv4ToString
        this.intIPv6 = IntIPv6(0L, 0L)
    }

    constructor(ipv6Address: IntIPv6) {
        this.ipVersion = IPVersion.IPv6
        this.intIPv6 = ipv6Address
        this.stringIP = intIPv6.convertIPv6ToString
        this.intIPv4 = 0
    }

    constructor(address: String, ipVersion: IPVersion) {
        this.ipVersion = ipVersion
        when (ipVersion) {
            IPVersion.IPv4 -> {
                this.intIPv4 = address.convertIPv4ToInt
                this.stringIP = address
                this.intIPv6 = IntIPv6(0L, 0L)
            }

            IPVersion.IPv6 -> {
                this.intIPv6 = address.convertIPv6ToInt
                this.stringIP = address
                this.intIPv4 = 0
            }
        }
    }

    override fun toString(): String = stringIP

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as IpAddress
        return stringIP == other.stringIP
    }

    override fun hashCode(): Int {
        return stringIP.hashCode()
    }

}