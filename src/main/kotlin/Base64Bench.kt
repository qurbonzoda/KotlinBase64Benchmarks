@file:Suppress("PrivatePropertyName", "ConstPropertyName")
@file:OptIn(kotlin.io.encoding.ExperimentalEncodingApi::class)

package bench

import org.openjdk.jmh.annotations.*
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import kotlin.io.encoding.decodingWith
import kotlin.io.encoding.encodingWith

private const val string4 = "Love"
private const val string10 = "LoveKotlin"
private val string100 = string10.repeat(10)
private val string1_000 = string100.repeat(10)
private val string10_000 = string1_000.repeat(10)
private val string100_000 = string10_000.repeat(10)
private val string1_000_000 = string100_000.repeat(10)

private val byteArray4 = string4.toByteArray()
private val byteArray10 = string10.toByteArray()
private val byteArray100 = string100.toByteArray()
private val byteArray1_000 = string1_000.toByteArray()
private val byteArray10_000 = string10_000.toByteArray()
private val byteArray100_000 = string100_000.toByteArray()
private val byteArray1_000_000 = string1_000_000.toByteArray()

@State(Scope.Benchmark)
@Fork(1)
@Measurement(iterations = 10, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Warmup(iterations = 10, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
open class Base64Bench {

    @Param("basic", "mime")
    var variant: String = ""

    @Param("4", "10", "100", "1000", "10000", "100000", "1000000")
    var length: Int = 0

    private var string: String = ""
    private var byteArray: ByteArray = byteArrayOf()

    private var buffer: ByteArray = byteArrayOf()

    private var kotlinBase64: kotlin.io.encoding.Base64 =  kotlin.io.encoding.Base64
    private var javaBase64Encoder =  java.util.Base64.getEncoder()
    private var javaBase64Decoder =  java.util.Base64.getDecoder()

    @Setup
    fun setUp() {
        when (length) {
            4 -> {
                string = string4
                byteArray = byteArray4
            }
            10 -> {
                string = string10
                byteArray = byteArray10
            }
            100 -> {
                string = string100
                byteArray = byteArray100
            }
            1_000 -> {
                string = string1_000
                byteArray = byteArray1_000
            }
            10_000 -> {
                string = string10_000
                byteArray = byteArray10_000
            }
            100_000 -> {
                string = string100_000
                byteArray = byteArray100_000
            }
            1_000_000 -> {
                string = string1_000_000
                byteArray = byteArray1_000_000
            }
            else -> {
                error("Unexpected length: $length")
            }
        }

        when (variant) {
            "basic" -> {
                kotlinBase64 = kotlin.io.encoding.Base64
                javaBase64Encoder = java.util.Base64.getEncoder()
                javaBase64Decoder = java.util.Base64.getDecoder()
            }
            "mime" -> {
                kotlinBase64 = kotlin.io.encoding.Base64.Mime
                javaBase64Encoder = java.util.Base64.getMimeEncoder()
                javaBase64Decoder = java.util.Base64.getMimeDecoder()
            }
            else -> {
                error("Unexpected Base64 variant: $variant")
            }
        }

        buffer = ByteArray(length * 2)
    }

    // encode

    @Benchmark
    fun kotlinEncode(): String {
        return kotlinBase64.encode(byteArray)
    }

    @Benchmark
    fun kotlinEncodeToByteArray(): ByteArray {
        return kotlinBase64.encodeToByteArray(byteArray)
    }

    @Benchmark
    fun javaEncode(): String {
        return javaBase64Encoder.encodeToString(byteArray)
    }

    @Benchmark
    fun javaEncodeToByteArray(): ByteArray {
        return javaBase64Encoder.encode(byteArray)
    }

    // decode

    @Benchmark
    fun kotlinDecode(): ByteArray {
        return kotlinBase64.decode(string)
    }

    @Benchmark
    fun kotlinDecodeFromByteArray(): ByteArray {
        return kotlinBase64.decode(byteArray)
    }

    @Benchmark
    fun javaDecode(): ByteArray {
        return javaBase64Decoder.decode(string)
    }

    @Benchmark
    fun javaDecodeFromByteArray(): ByteArray {
        return javaBase64Decoder.decode(byteArray)
    }

    // stream encode

    @Benchmark
    fun kotlinStreamEncode(): Int {
        val underlying = ByteArrayOutputStream(length * 2)

        val outputStream = underlying.encodingWith(kotlinBase64)
        for (byte in byteArray) {
            outputStream.write(byte.toInt())
        }
        outputStream.close()

        return underlying.size()

    }

    @Benchmark
    fun kotlinStreamEncodeBatch(): Int {
        val underlying = ByteArrayOutputStream(length * 2)

        val outputStream = underlying.encodingWith(kotlinBase64)
        outputStream.write(byteArray)
        outputStream.close()

        return underlying.size()
    }

    @Benchmark
    fun javaStreamEncode(): Int {
        val underlying = ByteArrayOutputStream(length * 2)

        val outputStream = javaBase64Encoder.wrap(underlying)
        for (byte in byteArray) {
            outputStream.write(byte.toInt())
        }
        outputStream.close()

        return underlying.size()
    }

    @Benchmark
    fun javaStreamEncodeBatch(): Int {
        val underlying = ByteArrayOutputStream(length * 2)

        val outputStream = javaBase64Encoder.wrap(underlying)
        outputStream.write(byteArray)
        outputStream.close()

        return underlying.size()
    }

    // stream decode

    @Benchmark
    fun kotlinStreamDecode(): Int {
        var result = 0

        val underlying = byteArray.inputStream()
        val outputStream = underlying.decodingWith(kotlinBase64)
        do {
            val read = outputStream.read()
            result = result xor read
        } while (read >= 0)
        outputStream.close()

        return result

    }

    @Benchmark
    fun kotlinStreamDecodeBatch(): Int {
        var result = 0

        val underlying = byteArray.inputStream()
        val outputStream = underlying.decodingWith(kotlinBase64)
        do {
            val read = outputStream.read(buffer)
            result = result xor read
        } while (read >= 0)
        outputStream.close()

        return result
    }

    @Benchmark
    fun javaStreamDecode(): Int {
        var result = 0

        val underlying = byteArray.inputStream()
        val outputStream = javaBase64Decoder.wrap(underlying)
        do {
            val read = outputStream.read()
            result = result xor read
        } while (read >= 0)
        outputStream.close()

        return result
    }

    @Benchmark
    fun javaStreamDecodeBatch(): Int {
        var result = 0

        val underlying = byteArray.inputStream()
        val outputStream = javaBase64Decoder.wrap(underlying)
        do {
            val read = outputStream.read(buffer)
            result = result xor read
        } while (read >= 0)
        outputStream.close()

        return result
    }
}