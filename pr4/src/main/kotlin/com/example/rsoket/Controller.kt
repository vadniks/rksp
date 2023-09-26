package com.example.rsoket

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.springframework.messaging.handler.annotation.MessageMapping
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@org.springframework.stereotype.Controller
class Controller(private val repo: Repository) {

    @MessageMapping("getAll") // stream
    fun getComponents() = Flux.create<Message> { emitter ->
        var index = 0
        repo.findAll().forEach {
            emitter.next(Message(true, it.toString(), index++))
        }
        emitter.complete()
    }

    @MessageMapping("addOne") // fire-n-forget
    fun addComponent(message: Message): Mono<Void> {
        runBlocking { launch {
            repo.save(Component.deserialized(message.payload))
        } }
        return Mono.empty()
    }

    @MessageMapping("getOne") // request-response
    fun getComponent(message: Message) = Mono.defer {
        val found = runBlocking { repo.findById(message.payload.toInt()) }
        Mono.just(Message(false, if (found.isPresent) found.get().serialized else ""))
    }
}
