@file:OptIn(kotlin.io.encoding.ExperimentalEncodingApi::class)

package bench

import kotlinx.benchmark.Benchmark
import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit
import kotlin.io.encoding.Base64

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
open class CharsToBytesBench {
    @Param("4", "10", "100", "1000", "10000", "100000", "1000000")
    var length: Int = 1000000

    private var string: String = ""
    private var byteArray: ByteArray = byteArrayOf()

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
    }

    @Benchmark
    fun charsToBytesDefault(): ByteArray {
        return charsToBytes(string, 0, length)
    }

    @Benchmark
    fun substringCharsToBytesDefault(): ByteArray {
        return charsToBytes(string, 1, length - 1)
    }

    @Benchmark
    fun charsToBytesGetBytes(): ByteArray {
        return charsToBytesGetBytes(string, 0, length)
    }

    @Benchmark
    fun substringCharsToBytesGetBytes(): ByteArray {
        return charsToBytesGetBytes(string, 1, length - 1)
    }

    @Benchmark
    fun bytesToCharsStringConstructorLatin1(): String {
        return String(byteArray, Charsets.ISO_8859_1)
    }

    @Benchmark
    fun bytesToCharsStringConstructorAscii(): String {
        return String(byteArray, Charsets.US_ASCII)
    }

    @Benchmark
    fun bytesToCharsStringBuilder(): String {
        val stringBuilder = StringBuilder(byteArray.size)
        for (byte in byteArray) {
            stringBuilder.append(byte.toInt().toChar())
        }
        return stringBuilder.toString()
    }

    private val destination = StringBuilder().apply {
        append(1)
        append("apple")
    }

    @Benchmark
    fun encodeToAppendableString(): Appendable {
        destination.clear()
        val stringResult = Base64.encode(byteArray)
        destination.append(stringResult)
        return destination
    }

    @Benchmark
    fun encodeToAppendableByteArray(): Appendable {
        destination.clear()
        val byteArrayResult = Base64.encodeToByteArray(byteArray)
        for (byte in byteArrayResult) {
            destination.append(byte.toInt().toChar())
        }
        return destination
    }
}

private fun charsToBytes(source: String, startIndex: Int, endIndex: Int): ByteArray {
    val byteArray = ByteArray(endIndex - startIndex)
    var length = 0
    for (index in startIndex until endIndex) {
        val symbol = source[index].code
        if (symbol <= 0xFF) {
            byteArray[length++] = symbol.toByte()
        } else {
            // the replacement byte must be an illegal symbol
            // so that mime skips it and basic throws with correct index
            byteArray[length++] = 0x3F
        }
    }
    return byteArray
}

@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
private fun charsToBytesGetBytes(source: String, startIndex: Int, endIndex: Int): ByteArray {
    return (source.substring(startIndex, endIndex) as java.lang.String).getBytes(Charsets.ISO_8859_1)
}
