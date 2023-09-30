
package com.example.rsocket

import jakarta.annotation.PostConstruct
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Order
import org.junit.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Lazy
import org.springframework.messaging.rsocket.RSocketRequester
import org.springframework.messaging.rsocket.RSocketStrategies
import org.springframework.messaging.rsocket.annotation.support.RSocketMessageHandler
import org.springframework.messaging.rsocket.connectTcpAndAwait
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit4.SpringRunner
import reactor.core.publisher.Flux
import reactor.test.StepVerifier
import reactor.util.retry.Retry
import java.time.Duration

@RunWith(SpringRunner::class)
@SpringBootTest(classes = [RsocketApplication::class])
@TestPropertySource(properties = ["spring.rsocket.server.port=7000"])
class RsocketApplicationTests {
    @Autowired private lateinit var builder: RSocketRequester.Builder
    @Autowired private lateinit var rSocketStrategies: RSocketStrategies
    private lateinit var rSocketRequester: RSocketRequester

    @PostConstruct
    fun init() {
        rSocketRequester = runBlocking {
            builder
                .setupRoute("connect")
                .setupData(System.currentTimeMillis())
                .rsocketConnector {
                    it.reconnect(Retry.fixedDelay(2, Duration.ofSeconds(10)))
                    it.acceptor(RSocketMessageHandler.responder(rSocketStrategies, Any()))
                }
                .connectTcpAndAwait("localhost", 7000)
        }
    }

    private fun assert(condition: Boolean) = assertDoesNotThrow { if (!condition) throw AssertionError() }

    private inline fun <T : Any> T.doAction(crossinline action: (T) -> Unit) = action(this)

    @Order(1)
    @Test
    fun test_fireNForget_addComponent() = rSocketRequester
        .route("addOne")
        .data(Message(false, Component(Component.Type.CPU, "cpu1", 1).serialized))
        .retrieveMono(Void::class.java)
        .doAction {
            StepVerifier
                .create(it)
                .verifyComplete()
        }

    @Order(2)
    @Test
    fun test_requestResponse_getComponent() = rSocketRequester
        .route("getOne")
        .data(Message(false, "1"))
        .retrieveMono(Message::class.java)
        .doAction { message ->
            StepVerifier
                .create(message)
                .consumeNextWith {
                    assert(!it.stream)
                    assert(it.payload != null)
                    assert(it.index == 0)

                    Component.deserialized(it.payload!!).apply {
                        assert(type == Component.Type.CPU)
                        assert(name == "cpu1")
                        assert(cost == 1)
                        assert(id == 1)
                    }
                }
                .verifyComplete()
        }

    @Order(3)
    @Test
    fun test_channel_addComponents() = rSocketRequester
        .route("addSeveral")
        .data(Flux.fromIterable(ArrayList<Message>().apply {
            add(Message(true, Component(Component.Type.MOTHERBOARD, "mb1", 2).serialized))
            add(Message(true, Component(Component.Type.RAM, "ram1", 3).serialized, 1))
            add(Message(true, Component(Component.Type.GPU, "gpu1", 4).serialized, 2))
        }))
        .retrieveFlux(Message::class.java)
        .doAction { result ->
            fun StepVerifier.Step<Message>.check(xType: Component.Type, xName: String, xIndex: Int) = consumeNextWith {
                assert(it.stream)
                assert(it.payload != null)
                assert(it.index == xIndex)

                Component.deserialized(it.payload!!).apply {
                    assert(type == xType)
                    assert(name == xName)
                    assert(cost == xIndex)
                    assert(id == xIndex)
                }
            }

            StepVerifier
                .create(result)
                .check(Component.Type.MOTHERBOARD, "mb1", 2)
                .check(Component.Type.RAM, "ram1", 3)
                .check(Component.Type.GPU, "gpu1", 4)
                .verifyComplete()
        }

    @Order(3)
    @Test
    fun test_stream_getComponents() = rSocketRequester
        .route("getAll")
        .retrieveFlux(Message::class.java)
        .doAction { StepVerifier.create(it).expectNextCount(4).verifyComplete() }
}
