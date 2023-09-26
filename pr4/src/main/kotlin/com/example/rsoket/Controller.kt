package com.example.rsoket

import org.springframework.messaging.handler.annotation.MessageMapping
import reactor.core.publisher.Flux

@org.springframework.stereotype.Controller
class Controller(private val repo: Repository) {
    private val server = "server"
    private val response = "response"
    private val stream = "stream"
    private val channel = "channel"

    @MessageMapping("getAll")
    fun getComponents() = Flux.create<Message> { emiter ->
        repo.findAll().forEach {
            emiter.next(Message(server, stream, it.toString()))
        }
        emiter.complete()
    }

    fun
}
