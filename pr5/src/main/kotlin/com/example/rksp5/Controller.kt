package com.example.rksp5

import jakarta.servlet.http.HttpServletRequest
import org.slf4j.Logger
import org.springframework.core.io.FileSystemResource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
class Controller(
    private val filesService: FilesService,
    private val logger: Logger,
    private val genericServerName: String,
    private val synchronizerService: SynchronizerService,
    private val currentServerId: Int
) {

    @GetMapping("/files")
    fun getAvailableFiles() = ResponseEntity.ok(filesService.getLocalFilesList())

    @GetMapping("/download/{name}")
    fun getFile(@PathVariable name: String, request: HttpServletRequest) = filesService.getLocalFile(name).run {
        request.getHeader(HttpHeaders.HOST).also {
            if (it.contains(genericServerName))
                logger.info("server $it requested synchronization of the $name file")
        }

        if (this != null) ResponseEntity.ok().body(FileSystemResource(this))
        else ResponseEntity.notFound()
    }

    @PostMapping("/upload/{name}")
    fun addFile(@PathVariable name: String, @RequestParam file: MultipartFile) {
        val added = filesService.addLocalFile(name, file.bytes)
        logger.info("file $name ${if (added) "added" else "not added"}")

        if (added) synchronizerService.synchronizeOthers(name)

        if (added) ResponseEntity.ok().body("")
        else ResponseEntity.status(409).body("File already exists")
    }

    @PostMapping("/notify/{serverId}/{file}")
    fun notifyFileAdded(
        @PathVariable serverId: String,
        @PathVariable file: String,
        request: HttpServletRequest
    ) = request.getHeader(HttpHeaders.HOST).run {
        val valid = this.contains(genericServerName)
        logger.info("notified ${if (valid) "properly" else "not properly"} about file addition")

        if (!valid) ResponseEntity.status(HttpStatus.FORBIDDEN)
        else synchronizerService.synchronizeSelf(serverId.toInt(), file).run { ResponseEntity.ok() }
    }

    @PostMapping("/halt")
    fun halt() = Runtime.getRuntime().halt(0)
}
