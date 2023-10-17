package com.example.rksp5

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean

@Suppress("unused")
@SpringBootApplication
class Rksp5Application {
    private val xLogger = LoggerFactory.getLogger(Rksp5Application::class.java)

    val logger: Logger @Bean(name = ["logger"]) get() = xLogger
    val genericServerName @Bean(name = ["genericServerName"]) get() = "rksp5server"
    val serverIds @Bean(name = ["serverIds"]) get() = IntArray(4) { it - 1 }
}

fun main(args: Array<String>) {
    SpringApplication.run(Rksp5Application::class.java, *args)
}
