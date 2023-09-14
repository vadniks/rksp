import P3T2.printed
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.ObservableEmitter
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
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

    fun run() {
        t211()
//        t212()
        t213()
    }

    private inline fun Int.generated(crossinline block: Flowable<Int>.() -> Unit) {
        var count = 0

        @Suppress("ReactiveStreamsUnusedPublisher")
        block(Flowable.generate {
            if (count++ < this)
                it.onNext(Random.nextInt(0, 1001))
            else
                it.onComplete()
        })

        println("\n")
    }

    private val Flowable<Int>.printed get() = forEach { println(it) }.dispose()

    private fun t211() = 1000.generated { map { it * it }.printed }
    private fun t212() = 1000.generated { filter { it > 500 }.printed }
    private fun t213() = Random.nextInt(0, 1000).also { print("$it ") }.generated { println(count().blockingGet()) }

    private fun t221() {
        
    }
}

fun main() {
//    P3T1.run()
    P3T2.run()
}

