[Language: English](./README.md)

# wirebare-kernel

wirebare-kernel 是一个基于 Android VPN Service 开发的 Android 网络代理框架

wirebare-kernel 不提供用户界面，它提供的是网络代理能力

另：基于 wirebare-kernel 开发出了两款网络代理类型的应用程序

- 网络抓包工具 [wirebare-android](https://github.com/Kokomi7QAQ/wirebare-android)
- 弱网测试工具 (开发中)


### 功能概览

#### 网际层

- 支持 IPv4 和 IPv6 的代理抓包
- 支持 IP 协议解析

#### 传输层

- 支持 TCP 透明代理、拦截抓包
- 支持 UDP 透明代理

#### 应用层

- 支持 HTTP 协议解析
- 支持 HTTPS 加解密（基于 TLSv1.2，需要先为 Android 安装代理服务器根证书）



### 注册代理服务

WireBare 代理服务是一个抽象类，你可以继承它然后进行自定义，SimpleWireBareProxyService 是它最简单的实现子类

```kotlin
class SimpleWireBareProxyService : WireBareProxyService()
```



在 AndroidManifest.xml 文件的 application 标签中添加如下代码来注册 WireBare 代理服务（以 SimpleWireBareProxyService 为例）

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



### 准备代理服务

在启动 WireBare 代理服务前需要先进行准备，第一次准备时将会弹出一个用户授权对话框，用户授权后即可启动代理服务

```kotlin
class SimpleActivity : VpnPrepareActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 若授权成功，则会回调 onPrepareSuccess()
        prepareProxy()
    }
    
    override fun onPrepareSuccess() {
        // 启动网络代理抓包
    }

}
```



### 配置和启动代理服务

准备过后即可随时启动 WireBare 代理服务，下面是详细的配置说明

```kotlin
fun start() {
    // 注册代理服务状态监听器，可以监听代理服务的启动和销毁以及代理服务器的启动
    // 需要注销，调用 WireBare.removeVpnProxyStatusListener(...)
    WireBare.addVpnProxyStatusListener(...)
    // 直接访问以下变量也可以随时获取代理服务的运行状态
    val vpnProxyServiceStatus = WireBare.proxyStatus
    
    // 配置 WireBare 日志等级
    WireBare.logLevel = Level.SILENT

    // 配置动态配置属性
    // 以下配置可以动态配置实时生效
    // 模拟丢包概率，取值范围 [0, 100]
    WireBare.dynamicConfiguration.mockPacketLossProbability = 0
    // 配置并启动代理服务
    // 以下配置需要修改时需要重启代理服务
    WireBare.startProxy {
        // 如果需要支持 HTTPS 抓包，则需要配置密钥信息
        jks = JKS(...)
        
        // 代理服务传输单元大小，单位：字节（默认 4096）
        mtu = 10 * 1024
        
        // TCP 代理服务器数量
        tcpProxyServerCount = 1
        
        // VpnService 的 IPv4 地址
        ipv4ProxyAddress = "10.1.10.1" to 32
        
        // 启用 IPv6 数据包代理
        enableIpv6 = true
        
        // VpnService 的 IPv6 地址
        ipv6ProxyAddress = "a:a:1:1:a:a:1:1" to 128
        
        // 增加代理服务的路由
        // 如果启用了 IPv6 数据包代理，则需要同时设置 IPv6 数据包的路由
        addRoutes("0.0.0.0" to 0, "::" to 0)
        
        // 增加 DNS 服务器
        addDnsServers(...)
        
        // 以下两种设置只能配置其中一种，不能同时配置
        // 母应用默认被代理，无需手动配置
        // 增加被代理的应用
        addAllowedApplications(...)
        // 增加不允许代理的应用
        addDisallowedApplications(...)
        
        // 增加异步 HTTP 拦截器
        addAsyncHttpInterceptor(...)
        // 增加阻塞 HTTP 拦截器
        addHttpInterceptor(...)
    }
}
```



### 停止代理服务

抓包完毕后，执行以下函数来停止代理服务

```kotlin
fun stop() {
    WireBare.stopProxy()
}
```

