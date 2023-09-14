
import io.reactivex.rxjava3.core.*
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.functions.Consumer
import io.reactivex.rxjava3.functions.Function
import io.reactivex.rxjava3.schedulers.Schedulers
import org.reactivestreams.Publisher
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.random.Random
import kotlin.reflect.KClass

object P3T1 {

    fun run() {
        data class Event(val which: Boolean, val value: Int)

        val stopped = AtomicBoolean(false)

        val observer = object : Observer<Event> {
            override fun onSubscribe(d: Disposable) = Unit
            override fun onError(e: Throwable) = Unit
            override fun onComplete() = Unit

            private var lastTemp = 0
            private var lastCo2 = 0

            override fun onNext(event: Event) {
                if (stopped.get()) return
                if (lastTemp > 25 && lastCo2 > 70) return

                if (event.which) {
                    lastTemp = event.value

                    if (lastTemp > 25)
                        println("temperature's too high ($lastTemp) ${Thread.currentThread().id}")
                } else {
                    lastCo2 = event.value

                    if (lastCo2 > 70)
                        println("carbon dioxide is too high ($lastCo2) ${Thread.currentThread().id}")
                }

                if (lastTemp > 25 && lastCo2 > 70) {
                    stopped.set(true)
                    println("alarm ($lastTemp, $lastCo2) ${Thread.currentThread().id}")
                } else
                    print("")
            }
        }

        fun observable(which: Boolean, it: ObservableEmitter<Event>) {
            while (!stopped.get()) {
                it.onNext(Event(
                    which,
                    if (which) Random.nextInt(15, 31)
                    else Random.nextInt(30, 101)
                ))
                Thread.sleep(1000)
            }
            it.onComplete()
        }

        val temperature = Observable.create<Event> { observable(true, it) }
        val carbonDioxide = Observable.create<Event> { observable(false, it) }

        Thread {
            temperature.subscribeOn(Schedulers.newThread()).observeOn(Schedulers.single()).subscribe(observer)
            carbonDioxide.observeOn(Schedulers.single()).subscribe(observer)
        }.apply {
            start()
            join()
        }
    }
}

object P3T2 {
    private const val size = 1000

    fun run() {
//        t211()
//        t212()
//        t213()
        t221()
    }

    private inline fun <T : Any> Int.generated(crossinline random: () -> T, crossinline block: Flowable<T>.() -> Unit) {
        var count = 0

        @Suppress("ReactiveStreamsUnusedPublisher")
        block(Flowable.generate {
            if (count++ < this)
                it.onNext(random())
            else
                it.onComplete()
        })

        println("\n")
    }

    private val Flowable<Int>.printed get() = forEach { println(it) }.dispose()
    private val intRandom: () -> Int get() = { Random.nextInt(0, 1001) }

    private fun t211() = size.generated(intRandom) { map { it * it }.printed }
    private fun t212() = size.generated(intRandom) { filter { it > 500 }.printed }

    private fun t213() = Random.nextInt(0, size)
        .also { print("$it ") }
        .generated(intRandom) { println(count().blockingGet()) }

    private fun t221() {
        val size = 10
        val chars = Array(size) { Pair(Char::class, 'a') }
        val ints = Array(size) { Pair(Int::class, 0) }

        for (i in 0 until size) {
            chars[i] = Pair(Char::class, /*Random.nextInt('a'.code, 'z'.code)*/('a'.code + i).toChar())
            ints[i] = Pair(Int::class, i/*Random.nextInt(0, 100)*/)
        }

        val executor = Executors.newSingleThreadExecutor()
        executor.submit {}

        val t1 = Schedulers.newThread()
        val t2 = Schedulers.newThread()
        val t3 = Schedulers.from(executor)

        val queueA = ArrayDeque<Char>()
        val queueB = ArrayDeque<Int>()
        val which = AtomicBoolean(false)

        Observable.merge<Pair<KClass<*>, Any>>(
            Observable.fromArray(*chars).subscribeOn(t1).observeOn(t3),
            Observable.fromArray(*ints).subscribeOn(t2).observeOn(t3)
        ).observeOn(t3).doOnComplete { t3.shutdown() }.subscribe { item ->
            if (item.first == Char::class) queueA.addLast(item.second as Char)
            else queueB.addLast(item.second as Int)

            if (!which.get()) {
                var next: Char?
                if (queueA.removeFirstOrNull().also { next = it } != null)
                    println(next)
                else
                    return@subscribe
                which.set(true)
            } else {
                var next: Int?
                if (queueB.removeFirstOrNull().also { next = it } != null)
                    println("\t" + next)
                else
                    return@subscribe
                which.set(false)
            }
        }

        executor.awaitTermination(10, TimeUnit.SECONDS)
    }
}

fun main() {
//    P3T1.run()
    P3T2.run()
}

