package top.sankokomi.wirebare.kernel.util

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.zip.GZIPInputStream

fun ByteBuffer.unzipGzip(): ByteBuffer {
    val compressedData: ByteBuffer = this
    return ByteBuffer.wrap(compressedData.array().unzipGzip())
}

fun ByteArray.unzipGzip(): ByteArray {
    val compressedData: ByteArray = this
    val compressedStream = ByteArrayInputStream(compressedData)
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