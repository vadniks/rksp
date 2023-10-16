package com.example.rksp5

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean

@SpringBootApplication
class Rksp5Application {
    private val xLogger = LoggerFactory.getLogger(Rksp5Application::class.java)

    @Suppress("unused")
    val logger: Logger @Bean(name = ["logger"]) get() = xLogger
}

fun main(args: Array<String>) {
    runApplication<Rksp5Application>(*args)
}
