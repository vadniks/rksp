
package com.example.rsoket

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import java.util.*

@SpringBootApplication
class RsoketApplication

fun main(args: Array<String>) {
    runApplication<RsoketApplication>(*args) {
        setDefaultProperties(Properties().apply {
            put("spring.session.jdbc.initialize-schema", "always")
            put("spring.rsocket.server.port", 7000)
            put("spring.main.lazy-initialization", true)
        })
    }
}
