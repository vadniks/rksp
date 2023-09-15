
import io.reactivex.rxjava3.core.*
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.PublishSubject
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.random.Random

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
//        t221()
//        t222()
        t223()
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

    }

    private fun t222() {
        val size = 10
        val array1 = Array(size) { 0 }
        val array2 = Array(size) { 0 }

        for (i in 0 until size) {
            array1[i] = Random.nextInt()
            array2[i] = Random.nextInt()
        }

        Flowable.concat(
            Flowable.fromArray(*array1),
            Flowable.fromArray(*array2)
        ).subscribe {
            println(it)
        }
    }

    private fun t223() {
        val stream1 = Observable.create<Pair<Boolean, Int>> {
            for (i in 0 until size)
                it.onNext(false to Random.nextInt())
            it.onComplete()
        }

        val stream2 = Observable.create<Pair<Boolean, Int>> {
            for (i in 0 until size)
                it.onNext(true to Random.nextInt())
            it.onComplete()
        }

        val executor = Executors.newSingleThreadExecutor()
        executor.submit {}

        val t1 = Schedulers.newThread()
        val t2 = Schedulers.newThread()
        val t3 = Schedulers.from(executor)

        val queue1 = ArrayDeque<Int>()
        val queue2 = ArrayDeque<Int>()
        val which = AtomicBoolean(false)

        var gatheredSize = 0
        var emittedCount = 0

        val result = PublishSubject.create<Int>()
        result.observeOn(Schedulers.newThread()).subscribe {
            println(it)
            if (++emittedCount >= gatheredSize)
                executor.shutdown()
        }

        fun divide() {
            if (!which.get()) {
                var next: Int?
                if (queue1.removeFirstOrNull().also { next = it } != null)
                    result.onNext(next!!)
                else
                    return
                which.set(true)
            } else {
                var next: Int?
                if (queue2.removeFirstOrNull().also { next = it } != null)
                    result.onNext(next!!)
                else
                    return
                which.set(false)
            }
        }

        fun onComplete() {
            for (i in 0 until queue1.size + queue2.size)
                divide()
        }

        Observable.merge(
            stream1.subscribeOn(t1).observeOn(t3),
            stream2.subscribeOn(t2).observeOn(t3)
        ).observeOn(t3).doOnComplete { onComplete() }.subscribe { item ->
            gatheredSize++

            if (!item.first) queue1.addLast(item.second)
            else queue2.addLast(item.second)

            divide()
        }
    }
}

fun main() {
//    P3T1.run()
    P3T2.run()
}

