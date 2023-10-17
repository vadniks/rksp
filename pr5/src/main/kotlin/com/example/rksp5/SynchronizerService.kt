package com.example.rksp5

import jakarta.annotation.PostConstruct
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Qualifier
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
    @Qualifier("serverIds") private val serverIds: IntArray,
    private val currentServerId: Int
) {

    @PostConstruct
    fun init() = logger.info("Server started")

    @Scheduled(fixedDelay = 60000, initialDelay = 60000)
    private fun synchronizeSelf() {
        logger.info("Beginning file synchronization...")

        var connectedToSomeone = false
        var hasSmthToSync = false

        for (serverId in serverIds) {
            if (serverId == currentServerId) continue

            val client = WebClient.builder()
                .baseUrl("http://$genericServerName$serverId:8080")
                .defaultHeader(HttpHeaders.HOST, "$genericServerName$currentServerId")
                .build()

            val remoteFiles = try {
                client.get()
                    .uri("/files")
                    .retrieve()
                    .bodyToMono(Array<String>::class.java)
                    .block()
            } catch (_: Exception) { null }

            if (remoteFiles == null) {
                logger.warn("Unable to connect to server$serverId")
                continue
            }

            connectedToSomeone = true

            for (remote in remoteFiles) {
                var found = false

                for (local in filesService.getLocalFilesList())
                    if (remote == local)
                        found = true

                if (!found) {
                    hasSmthToSync = true
                    retrieveMissingFile(serverId, remote)
                }
            }
        }

        if (!connectedToSomeone)
            logger.warn("Unable to connect to any server")
        else {
            if (!hasSmthToSync)
                logger.info("nothing to synchronize")
            else
                logger.info("files synchronized")
        }
    }

    fun synchronizeSelf(serverId: Int, file: String) {
        var found = false
        for (i in serverIds)
            if (i == serverId) {
                found = true
                break
            }
        if (!found) throw IllegalStateException()

        retrieveMissingFile(serverId, file)
    }

    fun synchronizeOthers(file: String) {
        logger.info("notifying others...")

        for (i in serverIds) {
            if (i == currentServerId) continue

            val client = WebClient.builder()
                .baseUrl("http://$genericServerName$i:8080")
                .defaultHeader(HttpHeaders.HOST, "$genericServerName$currentServerId")
                .build()

            val notified = try {
                client.post()
                    .uri("/notify/$currentServerId/$file")
                    .exchangeToMono { it.toBodilessEntity() }
                    .block()
                true
            } catch (_: Exception) { false }

            if (notified)
                logger.info("notified server$i")
            else
                logger.warn("unable to notify server$i")
        }
    }

    private fun retrieveMissingFile(serverId: Int, file: String) {
        logger.info("fetching missing file $file from server $genericServerName$serverId...")

        val client = WebClient.builder()
            .baseUrl("http://$genericServerName$serverId:8080")
            .defaultHeader(HttpHeaders.HOST, "$genericServerName$currentServerId")
            .exchangeStrategies(ExchangeStrategies.builder().codecs {
                it.defaultCodecs().maxInMemorySize(1.shl(31).toFloat().pow(2).toInt() * 2) // 2097152 bytes = 2 mb
            }.build())
            .build()

        val fileBytes = try {
            client.get()
                .uri("/download/$file")
                .retrieve()
                .bodyToMono(ByteArray::class.java)
                .block()
        } catch (_: Exception) { null }

        if (fileBytes == null) {
            logger.warn("Unable to retrieve file $file from server $genericServerName$serverId")
            return
        }

        if (!filesService.addLocalFile(file, fileBytes)) throw IllegalStateException()
        logger.info("file $file successfully retrieved")
    }
}
