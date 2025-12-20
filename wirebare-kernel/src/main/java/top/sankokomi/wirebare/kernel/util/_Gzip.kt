package top.sankokomi.wirebare.kernel.util

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.zip.GZIPInputStream

fun ByteBuffer.uncompressGzip(): ByteBuffer {
    return ByteBuffer.wrap(this.array().uncompressGzip())
}

fun ByteArray.uncompressGzip(): ByteArray {
    val compressedStream = ByteArrayInputStream(this)
    val gzipStream = GZIPInputStream(compressedStream)
    val outputStream = ByteArrayOutputStream()
    try {
        gzipStream.copyTo(outputStream)
    } finally {
        try {
            gzipStream.close()
            compressedStream.close()
        } catch (ignored: Exception) {
        }
    }
    return outputStream.toByteArray()
}