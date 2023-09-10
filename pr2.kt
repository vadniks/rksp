import org.apache.commons.io.FileUtils
import java.io.*
import java.lang.System.currentTimeMillis
import java.nio.ByteBuffer
import java.nio.file.*
import java.nio.file.StandardWatchEventKinds.*
import java.util.*
import java.util.stream.Collectors
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.io.path.readLines
import kotlin.math.round
import kotlin.random.Random

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
    private const val totalSize = (1 shl 20) * 100 // 1 mb * 100
    private const val originalFileName = "large1.bin"
    private const val copiedFileName = "large2.bin"

    private fun deleteFileIfExists(fileName: String) {
        File(fileName).apply {
            if (exists()) delete()
        }
    }

    private val callerMethodName get() = Thread.currentThread().stackTrace[3].methodName

    private inline fun timeMeasured(crossinline block: () -> Unit) {
        deleteFileIfExists(copiedFileName)
        val start = currentTimeMillis()
        block()
        println(callerMethodName + ' ' + (currentTimeMillis() - start) + "mc")
    }

    private fun copyIO() {
        timeMeasured {
            FileInputStream(originalFileName).use { input ->
                FileOutputStream(copiedFileName).use { output ->
                    var byte: Int
                    while (input.read().also { byte = it } != -1)
                        output.write(byte)
                }
            }
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
        timeMeasured {
            FileUtils.copyFile(File(originalFileName), File(copiedFileName))
        }
    }

    private fun copyFiles() {
        timeMeasured {
            Files.copy(Paths.get(originalFileName), Paths.get(copiedFileName))
        }
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

    @Deprecated("too slow")
    private fun createIO() {
        File(originalFileName).apply {
            if (exists()) return@apply

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
    }

    private fun createBuffered() {
        deleteFileIfExists(originalFileName)
        val writer = BufferedWriter(FileWriter(originalFileName), totalSize)

        val chunkSize = 1000
        var size = 0
        var state = 0
        while (size < totalSize) {
            writer.write(CharArray(chunkSize) { Char(1) })
            size += chunkSize
            state = progress("creation", size, totalSize, state)
        }
        println()
    }

    fun run() {
        createBuffered()

//        copyIO()
        copyFC()
//        copyApache()
//        copyFiles()
    }
}

object P2T3 {

    fun run() {
        val fileName = "some.txt"
        hashFile(fileName).also {
            print("hash of a $fileName file: $it (")

            for (i in (Byte.SIZE_BITS * Short.SIZE_BYTES - 1) downTo 0)
                print('0' + ((it.toInt() shr i) and 1))

            println(")")
        }
    }

    fun hashFile(fileName: String): Short {
        assert(Short.SIZE_BITS == 16)
        var hash: Short = 0
        val chunkSize = 100

        RandomAccessFile(fileName, "r").channel.use {
            var size = 0
            val buffer = ByteBuffer.allocate(chunkSize)

            while (size <= it.size()) {
                it.read(buffer)
                size += chunkSize

                for (i in buffer.array())
                    hash = ((Short.SIZE_BITS - 1) * hash + (i.toInt() and 0xff)).toShort()
            }
        }

        return hash
    }
}

object P2T4 {

    fun run() {
        val dirName = "watched"
        val timeout = 60_000 * 2

        val startedAt = currentTimeMillis()

        fun onFileCreated(path: Path) { println("${path.fileName} file created") }

        fun onFileModified(path: Path) {
            val name = path.fileName.toString()
            println("$name file modified ${path.toFile().exists()}")
        }

        fun onFileDeleted(path: Path) {
            val name = path.fileName.toString()
            println("$name file deleted")
        }

        fun <T> processEvent(kind: WatchEvent.Kind<T>, context: Any?) {
            if (kind.type().name != Path::class.java.name)
                throw IllegalStateException()

            context as Path?
            if (context == null) return

            when (kind) {
                ENTRY_CREATE -> onFileCreated(context)
                ENTRY_MODIFY -> onFileModified(context)
                ENTRY_DELETE -> onFileDeleted(context)
            }
        }

        fun processFileIfCan(action: File.() -> Unit) {
            action((File(dirName)
                .listFiles()
                ?.takeIf { it.isNotEmpty() } ?: return)[0])
        }

        Thread {
            while (currentTimeMillis() - startedAt < timeout / 4) {
                when (Random.nextInt(0, 3)) {
                    0 -> File(dirName, Random.nextInt().toString()).createNewFile()
                    1 -> processFileIfCan { writeText(Random.nextInt().toString()) }
                    else -> processFileIfCan { delete() }
                }
                Thread.sleep(1000)
            }
        }.start()

        Paths.get(dirName).also { dirPath ->
            if (!Files.isDirectory(dirPath))
                Files.createDirectory(dirPath)

            FileSystems.getDefault().newWatchService().also { watchService ->
                dirPath.register(watchService, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE)

                while (currentTimeMillis() - startedAt < timeout) {
                    val key = watchService.poll() ?: continue

                    @Suppress("UNCHECKED_CAST")
                    val queue = ArrayDeque<WatchEvent<*>>() as Queue<WatchEvent<*>>

                    for (event in key.pollEvents())
                        queue.add(event)
                    key.reset()

                    var event: WatchEvent<*>?
                    while (queue.poll().also { event = it } != null) {
                        processEvent(event!!.kind(), event!!.context())
                    }
                }
            }
        }
    }
}

fun main() {
//    P2T1.run()
//    P2T2.run()
//    P2T3.run()
    P2T4.run()
}
