import java.io.BufferedInputStream
import java.lang.System.currentTimeMillis
import java.util.*
import java.util.concurrent.*
import kotlin.collections.ArrayDeque
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.math.sqrt
import kotlin.random.Random

object T1 {

    fun run() {
        val size = 10000
        val array = IntArray(size)

        for (i in array)
            array[i] = Random.nextInt()

        maxSeq(array.clone())
        maxMulti(array.clone())
        maxFJ(array.clone())
    }

    private fun maxSeq(array: IntArray) {
        val start = currentTimeMillis()

        var max = 0
        for (i in array) {
            Thread.sleep(1)
            if (i > max) {
                max = i
            }
        }

        println("maxSeq: $max ${currentTimeMillis() - start}ms")
    }

    private fun maxMulti(array: IntArray) {
        val start = currentTimeMillis()

        val quarter = array.size / 4
        var m1 = 0
        var m2 = 0
        var m3 = 0
        var m4 = 0

        val t1 = Thread {
            for (i in 0..quarter) {
                val j = array[i]
                Thread.sleep(1)
                if (j > m1) {
                    m1 = j
                }
            }
        }

        val t2 = Thread {
            for (i in quarter..(quarter * 2)) {
                val j = array[i]
                Thread.sleep(1)
                if (j > m2) {
                    m2 = j
                }
            }
        }

        val t3 = Thread {
            for (i in (quarter * 2)..(array.size - quarter)) {
                val j = array[i]
                Thread.sleep(1)
                if (j > m3) {
                    m3 = j
                }
            }
        }

        val t4 = Thread {
            for (i in (quarter * 3) until array.size) {
                val j = array[i]
                Thread.sleep(1)
                if (j > m4) {
                    m4 = j
                }
            }
        }

        t1.start()
        t2.start()
        t3.start()
        t4.start()

        t1.join()
        t2.join()
        t3.join()
        t4.join()

        val result = intArrayOf(m1, m2, m3, m4)
        var mr = 0
        for ((i, _) in result.withIndex()) {
            val j = result[i]
            Thread.sleep(1)
            if (j > mr) {
                mr = j
            }
        }

        println("maxMulti: $mr ${currentTimeMillis() - start}ms")
    }

    private fun maxFJ(original: IntArray) {
        val start = currentTimeMillis()

        class Task(
            private val array: IntArray,
            private val range: IntRange,
            private val index: Int
        ) : RecursiveTask<Int>() {

            override fun compute(): Int {
                if (index == 0) {
                    val quarter = array.size / 4

                    val t1 = Task(array, 0..quarter, 1).fork()
                    val t2 = Task(array, quarter..(quarter * 2), 2).fork()
                    val t3 = Task(array, (quarter * 2)..(array.size - quarter), 3).fork()
                    val t4 = Task(array, (quarter * 3) until array.size, 4).fork()

                    val arr = intArrayOf(
                        t1.join(),
                        t2.join(),
                        t3.join(),
                        t4.join()
                    )

                    return Task(arr, 0..3, 5).fork().join()
                } else
                    return max()
            }

            private fun max(): Int {
                var max = 0

                for (i in range) {
                    val j = array[i]
                    Thread.sleep(1)
                    if (j > max) {
                        max = j
                    }
                }

                return max
            }
        }

        val result = Task(original, 0..0, 0).fork().join()
        println("maxFJ: $result ${currentTimeMillis() - start}ms")
    }
}

object T2 {

    fun run() {
        val inn = System.`in` as BufferedInputStream
        val executor = ForkJoinPool()
        val tasks = ArrayList<Future<Int>>()

        while (true) {
            @Suppress("UNCHECKED_CAST")
            for (i in (tasks.clone() as ArrayList<Future<Int>>)) {
                if (i.isDone) {
                    val result = i.get()
                    tasks.remove(i)
                    Thread { println("result ${sqrt(result.toDouble()).toInt()} ^ 2 = $result") }.start()
                }
            }

            var availableBytes = 0
            availableBytes = inn.available()
            if (availableBytes == 0) continue

            val bytes = ByteArray(availableBytes)
            for (i in 0 until availableBytes)
                bytes[i] = inn.read().toByte()

            val input = String(bytes.slice(0..(bytes.size - 2)).toByteArray())
            if (input == "q") break
            val number = input.toInt()

            @Suppress("UNCHECKED_CAST")
            val task: Future<Int> = executor.submit(Callable {
                Thread.sleep(Random.nextLong(1, 5) * 1000L)
                number * number
            })
            tasks.add(task)
        }

        @Suppress("ControlFlowWithEmptyBody")
        for (i in tasks) while (!i.isDone);
    }
}

object T3 {

    fun run() {
        val xml = "xml"
        val json = "json"
        val xls = "xls"
        data class AkaFile(val name: String, val type: String, val size: Int)

        val startedAt = currentTimeMillis()
        val timeout = 10000

        val maxFiles = 5
        val queue = java.util.ArrayDeque<AkaFile>() as Queue<AkaFile>
        val lock = Any()

        val checker = HashMap<AkaFile, Boolean>()

        fun queueFull(): Boolean {
            var full: Boolean
            synchronized(lock) { full = queue.size >= maxFiles }
            return full
        }

        fun timeoutNotExceeded(): Boolean { return currentTimeMillis() - startedAt < timeout  }

        val generatorThread = Thread {
            while (timeoutNotExceeded()) {
                if (queueFull()) continue

                val akaFile = AkaFile(
                    Random.nextBytes(10).hashCode().toString(),
                    when (Random.nextInt(0, 2)) {
                        0 -> xml
                        1 -> json
                        else -> xls
                    },
                    Random.nextInt(10, 100)
                )

                synchronized(lock) {
                    queue.add(akaFile)
                    checker[akaFile] = false
                }

                println("generated file " + akaFile.name + '.' + akaFile.type)
                Thread.sleep(Random.nextLong(1L, 10L) * 100L)
            }
        }

        fun queueEmpty(): Boolean {
            var empty: Boolean
            synchronized(lock) { empty = queue.isEmpty() }
            return empty
        }

        val consumer = Runnable {
            while (timeoutNotExceeded()) {
                if (queueEmpty()) continue

                var akaFile: AkaFile?
                synchronized(lock) { akaFile = queue.poll() }
                if (akaFile == null) continue
                synchronized(lock) { checker[akaFile!!] = true }

                println("consumed file " + akaFile!!.name + '.' + akaFile!!.type)
                Thread.sleep((akaFile!!.size * 7).toLong())
            }
        }

        generatorThread.start()
        val xmlConsumerThread = Thread(consumer).apply { start() }
        val jsonConsumerThread = Thread(consumer).apply { start() }
        val xlsConsumerThread = Thread(consumer).apply { start() }

        generatorThread.join()
        xmlConsumerThread.join()
        jsonConsumerThread.join()
        xlsConsumerThread.join()

        println("queue size " + queue.size)
        for (i in checker)
            if (!i.value)
                throw IllegalStateException()
    }
}

fun main() {
    T1.run()
//    T2.run()
//    T3.run()
}
