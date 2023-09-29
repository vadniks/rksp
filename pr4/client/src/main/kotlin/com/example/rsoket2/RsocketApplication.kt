package com.example.rsoket2

import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.messaging.rsocket.*
import org.springframework.messaging.rsocket.annotation.support.RSocketMessageHandler
import reactor.core.Disposable
import reactor.core.publisher.Flux
import java.lang.RuntimeException

@SpringBootApplication
class RsocketApplication(
    private val rsocketRequesterBuilder: RSocketRequester.Builder,
    @Qualifier("rSocketStrategies") private val rsocketStrategies: RSocketStrategies
) {
    private lateinit var rsocketRequester: RSocketRequester
    private val id = System.currentTimeMillis()
    private lateinit var disposable: Disposable

    init {
        connect()
        getComponents().forEach { println(it) }
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
        assert(connected && this::disposable.isInitialized)
        println("stopping...")
        disposable.dispose()
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

        disposable = rsocketRequester
            .route("getAll")
            .data(Message(false, ""))
            .retrieveFlux(Message::class.java)
            .subscribe { components.add(Component.deserialized(it.payload)) }

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

    private fun getComponent(id: Int): Message { // request-response
        assert(connected)
        return rsocketRequester
            .route("getOne")
            .data(Message(false, id.toString()))
            .retrieveMono(Message::class.java)
            .block()!!
    }

    private fun adcComponents(components: List<Component>) { // channel
        assert(connected)
        rsocketRequester
            .route("addSeveral")
            .data(Flux.fromIterable(components))
            .retrieveFlux(Message::class.java)
            .subscribe { println("added " + it.payload) }
    }
}

fun main(args: Array<String>) {
    runApplication<RsocketApplication>(*args)
}
