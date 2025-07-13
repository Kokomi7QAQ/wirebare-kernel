[语言：简体中文](./README_CN.md)

# wirebare-kernel

wirebare-kernel provides network packet capture framework.

wirebare-kernel doesn't provide UI.



If you want an Android App to capture network packet, see:

- Network packet capture App [wirebare-android](https://github.com/Kokomi7QAQ/wirebare-android)


### Features Overview

#### Network Layer

- Support IPv4 and IPv6
- Support parsing IP packet

#### Transport Layer

- Support transparent proxy and parsing TCP packet
- Support transparent proxy UDP packet

#### Application Layer

- Support parsing HTTP packet
- Support parsing HTTPS packet (Based on TLSv1.2, and the certificate must be installed first)



### Register WireBare Service

WireBareProxyService is an abstract class, you can implement it. SimpleWireBareProxyService is a simple subclass of it.

```kotlin
class SimpleWireBareProxyService : WireBareProxyService()
```



Add the following code into AndroidManifest.xml to register WireBare Service (e.g. SimpleWireBareProxyService).

```xml
<application>
    <service
        android:name="top.sankokomi.wirebare.kernel.service.SimpleWireBareProxyService"
        android:exported="false"
        android:permission="android.permission.BIND_VPN_SERVICE">
        <intent-filter>
            <action android:name="android.net.VpnService" />
            <action android:name="top.sankokomi.wirebare.kernel.action.Start" />
            <action android:name="top.sankokomi.wirebare.kernel.action.Stop" />
        </intent-filter>
    </service>
</application>
```



### Prepare WireBare Service

Before launching WireBare Service, we should request the VPN permission firstly.

```kotlin
class SimpleActivity : VpnPrepareActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // If the permission is granted, the function `onPrepareSuccess()` will be callback.
        prepareProxy()
    }
    
    override fun onPrepareSuccess() {
        // Launch WireBare Service here.
    }

}
```



### Config & launch WireBare Service

Following are the configuration details.

```kotlin
fun start() {
    // Register a listener to monitor the status.
    // To avoid memory leaks, you need to unregister by calling WireBare.removeVpnProxyStatusListener(...).
    WireBare.addVpnProxyStatusListener(...)
    // You can also get the status by the following code.
    val vpnProxyServiceStatus = WireBare.proxyStatus
    
    // Config log level.
    WireBare.logLevel = Level.SILENT

    // Config dynamic properties.
    // The following configurations can be dynamically configured to take effect in real time.
    // The probability of packet loss probability, value range: [0, 100]
    WireBare.dynamicConfiguration.mockPacketLossProbability = 0

    // Launch WireBare Service.
    // The following configuration must be configured at launching.
    WireBare.startProxy {
        // If you need to capture HTTPS packet, you must configure JKS.
        jks = JKS(...)
        
        // Set the maximum transmission unit (MTU) of the VPN interface, UNIT：Byte(4096 by default).
        mtu = 10 * 1024
        
        // Number of TCP proxy server.
        tcpProxyServerCount = 1
        
        // The IPv4 address of VpnService.
        ipv4ProxyAddress = "10.1.10.1" to 32
        
        // Enable IPv6, make sure the network supports IPv6.
        enableIpv6 = true
        
        // The IPv6 address of VpnService.
        ipv6ProxyAddress = "a:a:1:1:a:a:1:1" to 128
        
        // The routes of VpnService. Including IPv4 and IPv6.
        addRoutes("0.0.0.0" to 0, "::" to 0)
        
        // The DNS servers of VpnService.
        addDnsServers(...)
        
        // You can only configure one of the following two settings.
        // The parent App is proxied by default, no manual configuration is required.
        // The App package that you want to proxy.
        addAllowedApplications(...)
        // The App package that you don't want to proxy.
        addDisallowedApplications(...)
        
        // The async HTTP interceptor.
        addAsyncHttpInterceptor(...)
        // The sync HTTP interceptor.
        addHttpInterceptor(...)
    }
}
```



### Stop WireBare Service

Stop WireBare Service by the following code.

```kotlin
fun stop() {
    WireBare.stopProxy()
}
```

