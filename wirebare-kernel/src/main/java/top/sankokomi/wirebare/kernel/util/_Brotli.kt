package top.sankokomi.wirebare.kernel.util

import org.brotli.dec.BrotliInputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

fun ByteBuffer.uncompressBrotli(): ByteBuffer {
    return ByteBuffer.wrap(this.array().uncompressBrotli())
}

fun ByteArray.uncompressBrotli(): ByteArray {
    val compressedStream = ByteArrayInputStream(this)
    val brotliStream = BrotliInputStream(compressedStream)
    val outputStream = ByteArrayOutputStream()
    try {
        brotliStream.copyTo(outputStream)
    } finally {
        try {
            brotliStream.close()
            compressedStream.close()
        } catch (ignored: Exception) {
        }
    }
    return outputStream.toByteArray()
}