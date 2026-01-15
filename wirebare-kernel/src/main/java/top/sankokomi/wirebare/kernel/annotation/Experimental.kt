package top.sankokomi.wirebare.kernel.annotation

/**
 * 标记一个功能是实验性的，可能存在漏洞
 * */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.FIELD)
annotation class Experimental
