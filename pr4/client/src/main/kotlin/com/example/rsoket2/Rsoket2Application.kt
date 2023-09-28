package com.example.rsoket2

import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.messaging.rsocket.*
import org.springframework.messaging.rsocket.annotation.support.RSocketMessageHandler
import reactor.core.Disposable
import reactor.core.publisher.Flux

@SpringBootApplication
class Rsoket2Application(
    private val rsocketRequesterBuilder: RSocketRequester.Builder,
    @Qualifier("rSocketStrategies") private val rsocketStrategies: RSocketStrategies
) {
    private lateinit var rsocketRequester: RSocketRequester
    private val id = System.currentTimeMillis()
    private lateinit var disposable: Disposable

    private fun connect() = runBlocking {
        rsocketRequester = rsocketRequesterBuilder
            .setupRoute("connect")
            .setupData(id)
            .rsocketConnector { it.acceptor(RSocketMessageHandler.responder(rsocketStrategies)) }
            .connectTcpAndAwait("localhost", 7000)

        rsocketRequester.rsocket()!!
            .onClose()
            .doFinally { println("disconnected") }
            .subscribe()
    }

    private fun connected() = this::rsocketRequester.isInitialized || rsocketRequester.rsocket().isDisposed

    private fun stopStreamsAndChannels() {
        if (!connected() || !this::disposable.isInitialized)
            return
        println("stopping...")
        disposable.dispose()
    }

    private fun disconnect() {
        if (!connected()) return
        stopStreamsAndChannels()
        rsocketRequester.rsocket()!!.dispose()
        println("disconnected")
    }

    private fun getComponents(): List<Component> { // stream
        val components = ArrayList<Component>() // TODO: make payload in Component nullable
        if (!connected()) return components

        disposable = rsocketRequester
            .route("getAll")
            .data(Message(false, ""))
            .retrieveFlux(Message::class.java)
            .subscribe { components.add(Component.deserialized(it.payload)) }
    }

    private fun addComponent(component: Component) { // fire-n-forget
        if (!connected()) return // TODO: replace with assert

        rsocketRequester
            .route("addOne")
            .data(Message(false, component.serialized))
            .send()
            .block()
    }

    private fun getComponent(id: Int): Message { // request-response
        if (!connected()) return
        return rsocketRequester
            .route("getOne")
            .data(Message(false, id.toString()))
            .retrieveMono(Message::class.java)
            .block()!!
    }

    private fun adcComponents(components: List<Component>) { // channel
        if (!connected()) return

        rsocketRequester
            .route("addSeveral")
            .data(Flux.fromIterable(components))
            .retrieveFlux(Message::class.java)
            .subscribe { println("added " + it.payload) }
    }
}

fun main(args: Array<String>) {
    runApplication<Rsoket2Application>(*args)
}
