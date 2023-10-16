package com.example.rksp5

import jakarta.annotation.PostConstruct
import org.slf4j.Logger
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

@Service
class SynchronizerService(private val logger: Logger, private val filesService: FilesService) {
    private val servers = IntArray(4) { it + 1 }
    private val restTemplate = RestTemplate(HttpComponentsClientHttpRequestFactory())

    @PostConstruct
    fun init() = logger.info("Server started")

    @Scheduled(fixedDelay = 60000)
    fun synchronize() = servers.forEach { i ->
        val url = "http://rksp5server$i/files"

        restTemplate.getForObject(url, Array::class.java)?.forEach { remoteFile ->
            (remoteFile as String?)!!
            var foundLocally = false

            for (localFile in filesService.getLocalFilesList()) {
                if (remoteFile == localFile) {
                    foundLocally = true
                    break
                }
            }

//            if (!foundLocally)
//                restTemplate.getForEntity(url, ByteArrayResource())
        }
    }
}
