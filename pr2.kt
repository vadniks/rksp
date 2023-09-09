import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.lang.System.currentTimeMillis
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.math.round

object P2T1 {

    fun run() {
        val fileName = "some.txt"
        val filePath = Paths.get(fileName)

        if (!Files.exists(filePath)) {
            Files.createFile(filePath)

            RandomAccessFile(fileName, "rw").use { raf ->
                raf.channel.use { it.write(ByteBuffer.wrap("Hello World!".encodeToByteArray())) }
            }
        }

        ByteBuffer.allocate(32).apply {
            RandomAccessFile(fileName, "r").use { raf ->
                raf.channel.use { it.read(this) }
            }
            array().run { String(this) }.also { println(it) }
        }
    }
}

object P2T2 {
    private val totalSize = (1 shl 20) * 100 // 1 mb * 100
    private val originalFileName = "large1.bin"
    private val copiedFileName = "large2.bin"

    private fun deleteCopiedIfExists() {
        File(copiedFileName).apply {
            if (exists()) delete()
        }
    }

    private val callerMethodName get() = Thread.currentThread().stackTrace[3].methodName

    private inline fun timeMeasured(crossinline block: () -> Unit) {
        deleteCopiedIfExists()
        val start = currentTimeMillis()
        block()
        println(callerMethodName + ' ' + (currentTimeMillis() - start))
    }

    private fun copyIO() {
        timeMeasured {
            val input = FileInputStream(originalFileName)
            val output = FileOutputStream(copiedFileName)

            var byte: Int
            while (input.read().also { byte = it } != -1)
                output.write(byte)

            input.close()
            output.close()
        }
    }

    private fun copyFC() {
        timeMeasured {
            RandomAccessFile(originalFileName, "r").channel.use { original ->
                RandomAccessFile(copiedFileName, "rw").channel.use { copied ->
                    copied.transferFrom(original, 0, original.size())
                }
            }
        }
    }

    private fun copyApache() {

    }

    private fun progress(what: String, current: Int, total: Int, state: Int = 0): Int {
        print("\r$what " + round(current.toFloat() / total.toFloat() * 100f).toInt() + '%')
        print(' ')
        when (state) {
            0 -> {
                print('-')
                return 1
            }
            1 -> {
                print('\\')
                return 2
            }
            2 -> {
                print('|')
                return 3
            }
            3 -> {
                print('/')
                return 0
            }
            else -> throw IllegalStateException()
        }
    }

    fun run() {
        File(originalFileName).apply {
            if (exists()) return

            outputStream().use {
                var state = 0
                for (i in 0..totalSize) {
                    it.write(1)
                    if (i % 1000 == 0)
                        state = progress("creation", i, totalSize, state)
                }
            }
            println()
        }

        copyIO()
    }
}

fun main() {
//    P2T1.run()
    P2T2.run()
}
