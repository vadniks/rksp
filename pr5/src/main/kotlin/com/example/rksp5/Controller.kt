package com.example.rksp5

import jakarta.servlet.http.HttpServletRequest
import org.slf4j.Logger
import org.springframework.core.io.FileSystemResource
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
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
    private val genericServerName: String
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

    @PostMapping("/upload")
    fun addFile(@RequestParam file: MultipartFile) =
        if (filesService.addLocalFile(file.name, file.bytes)) ResponseEntity.ok().body("")
        else ResponseEntity.status(409).body("File already exists")

    @DeleteMapping("/delete/{name}")
    fun deleteFile(@PathVariable name: String) =
        if (filesService.removeLocalFile(name)) ResponseEntity.ok() else ResponseEntity.notFound()
}
