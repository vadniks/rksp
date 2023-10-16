package com.example.rksp5

import org.springframework.core.io.ByteArrayResource
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
class Controller(private val filesService: FilesService) {

    @GetMapping("/files")
    fun getAvailableFiles() = ResponseEntity.ok(filesService.getLocalFilesList())

    @GetMapping("/download/{name}")
    fun getFile(@PathVariable name: String) = filesService.getLocalFile(name).run {
        if (this != null) ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .header("Content-Disposition", "attachment; filename=\"$name\"")
            .header("Cache-Control", "no-cache, no-store, must-revalidate")
            .header("Pragma", "no-cache")
            .header("Expires", "0")
            .contentLength(this.length())
            .body(ByteArrayResource(this.readBytes()))
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
