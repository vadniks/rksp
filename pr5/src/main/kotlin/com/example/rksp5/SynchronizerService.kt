package com.example.rksp5

import jakarta.annotation.PostConstruct
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import kotlin.math.pow

@Suppress("HttpUrlsUsage")
@Service
class SynchronizerService(
    private val filesService: FilesService,
    private val logger: Logger,
    private val genericServerName: String,
    private val serverIds: IntArray
) {

    @Value("\${SERVER_ID}")
    private lateinit var currentServerId: String

    @PostConstruct
    fun init() = logger.info("Server started")

    @Scheduled(fixedDelay = 60000)
    fun synchronize() {
        logger.info("Beginning file synchronization...")

        var connectedToSomeone = false

        for (serverId in serverIds) {
            if (serverId == currentServerId.toInt()) continue

            val client = WebClient.builder()
                .baseUrl("http://$genericServerName$serverId")
                .defaultHeader(HttpHeaders.HOST, "$genericServerName$currentServerId")
                .build()

            val remoteFiles = client.get()
                .uri("/files")
                .retrieve()
                .bodyToMono(Array<String>::class.java)
                .block() ?: continue

            connectedToSomeone = true

            for (remote in remoteFiles) {
                var found = false

                for (local in filesService.getLocalFilesList())
                    if (remote == local)
                        found = true

                if (!found)
                    retrieveMissingFile(serverId, remote)
            }
        }

        if (!connectedToSomeone)
            logger.warn("Unable to connect to any server")
    }

    private fun retrieveMissingFile(serverId: Int, file: String) {
        logger.info("fetching missing file $file from server $genericServerName$serverId...")

        val client = WebClient.builder()
            .baseUrl("http://$genericServerName$serverId")
            .defaultHeader(HttpHeaders.HOST, "$genericServerName$currentServerId")
            .exchangeStrategies(ExchangeStrategies.builder().codecs {
                it.defaultCodecs().maxInMemorySize(1.shl(31).toFloat().pow(2).toInt() * 2) // 2097152 bytes = 2 mb
            }.build())
            .build()

        val fileBytes = client.get()
            .uri("/download/$file")
            .retrieve()
            .bodyToMono(ByteArray::class.java)
            .block()

        if (fileBytes == null) {
            logger.warn("Unable to retrieve file $file from server $genericServerName$serverId")
            return
        }

        if (!filesService.addLocalFile(file, fileBytes)) throw IllegalStateException()
        logger.info("file $file successfully retrieved")
    }
}
