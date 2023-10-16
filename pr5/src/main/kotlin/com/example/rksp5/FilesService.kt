package com.example.rksp5

import org.springframework.stereotype.Service
import java.io.File
import java.nio.file.Paths
import java.util.ArrayList

@Service
class FilesService {
    private val filesDir get() = Paths.get("/files").toFile()

    fun getLocalFilesList() = filesDir.listFiles()?.map { it.name } ?: ArrayList(0)

    fun getLocalFile(name: String) = File(filesDir, name).takeIf { it.exists() }

    fun addLocalFile(name: String, bytes: ByteArray) = File(filesDir, name).run {
        if (this.exists()) false
        else
            if (this.createNewFile()) { writeBytes(bytes); true }
            else false
    }

    fun removeLocalFile(name: String) = File(filesDir, name).run { if (exists()) delete() else false }
}
