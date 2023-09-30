
@file:Suppress("ControlFlowWithEmptyBody")

package com.example.rsoket2

import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.messaging.rsocket.*
import org.springframework.messaging.rsocket.annotation.support.RSocketMessageHandler
import reactor.core.publisher.Flux
import java.lang.RuntimeException
import java.util.Scanner
import java.util.concurrent.atomic.AtomicBoolean

@SpringBootApplication
class RsocketApplication(
    private val rsocketRequesterBuilder: RSocketRequester.Builder,
    @Qualifier("rSocketStrategies") private val rsocketStrategies: RSocketStrategies
) {
    private lateinit var rsocketRequester: RSocketRequester
    private val id = System.currentTimeMillis()

    init {
        connect()

        val scanner = Scanner(System.`in`)
        println("enter command:")

        while (true) {
            when (scanner.next()) {
                "q" -> break
                "get" -> getComponents().forEach { println(it.serialized) }
                "add" -> {
                    println("enter component in serialized form:")
                    addComponent(Component.deserialized(scanner.next()))
                }
                "specified" -> {
                    println("enter component's id to fetch:")
                    println(getComponent(scanner.nextInt()))
                }
                "several" -> {
                    println("enter count of components to add:")
                    val count = scanner.nextInt()
                    val list = ArrayList<Component>(count)

                    for (i in 0 until count) {
                        println("enter component in serialized form:")
                        list.add(Component.deserialized(scanner.next()))
                    }

                    addComponents(list)
                }
                else -> assert(false)
            }
            println("enter command:")
        }

        disconnect()
    }

    private fun assert(condition: Boolean) { if (!condition) throw RuntimeException() }

    private fun connect() = runBlocking {
        rsocketRequester = rsocketRequesterBuilder
            .setupRoute("connect")
            .setupData(id)
            .rsocketConnector { it.acceptor(RSocketMessageHandler.responder(rsocketStrategies, Any())) }
            .connectTcpAndAwait("localhost", 7000)

        rsocketRequester.rsocket()!!
            .onClose()
            .doFinally { println("disconnected") }
            .subscribe()
    }

    private val connected get() = this::rsocketRequester.isInitialized || rsocketRequester.rsocket()!!.isDisposed

    private fun stopStreamsAndChannels() {
        assert(connected)
        println("stopping...")
    }

    private fun disconnect() {
        assert(connected)
        stopStreamsAndChannels()
        rsocketRequester.rsocket()!!.dispose()
        println("disconnected")
    }

    private fun getComponents(): List<Component> { // stream
        assert(connected)
        val components = ArrayList<Component>() // TODO: make payload in Component nullable
        val condition = AtomicBoolean(false)

        rsocketRequester
            .route("getAll")
            .data(Message(false, ""))
            .retrieveFlux(Message::class.java)
            .doOnComplete { condition.set(true) }
            .subscribe { components.add(Component.deserialized(it.payload)) }

        while (!condition.get());
        return components
    }

    private fun addComponent(component: Component) { // fire-n-forget
        assert(connected)

        rsocketRequester
            .route("addOne")
            .data(Message(false, component.serialized))
            .send()
            .block()
    }

    private fun getComponent(id: Int): Component { // request-response
        assert(connected)
        return Component.deserialized(rsocketRequester
            .route("getOne")
            .data(Message(false, id.toString()))
            .retrieveMono(Message::class.java)
            .block()!!
            .payload)
    }

    private fun addComponents(components: List<Component>) { // channel // (10,CPU,cpu10,10) (20,CPU,cpu20,20)
        assert(connected)
        val condition = AtomicBoolean(false)

        var index = 0
        val flux = Flux.fromIterable(components).map { Message(true, it.serialized, index++) }

        rsocketRequester
            .route("addSeveral")
            .data(flux)
            .retrieveFlux(Message::class.java)
            .doOnComplete { condition.set(true) }
            .subscribe { println("added " + it.payload) }

        while (!condition.get());
    }
}

fun main(args: Array<String>) {
    runApplication<RsocketApplication>(*args)
}
