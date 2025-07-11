//
// Copyright (c) 2024 Couchbase, Inc All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
package com.couchbase.lite.color.util

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipInputStream


fun eraseFileOrDir(fileOrDirectory: String?) = fileOrDirectory?.let { File(it).eraseFileOrDir() } ?: true

fun File.eraseFileOrDir() = this.deleteRecursive()

fun deleteContents(fileOrDirectory: String?) = fileOrDirectory?.let { File(it).deleteContents() } ?: true

fun File.deleteContents(): Boolean {
    if (!this.isDirectory) {
        return true
    }

    val contents = this.listFiles() ?: return true

    var succeeded = true
    for (file in contents) {
        if (!file.deleteRecursive()) {
            succeeded = false
        }
    }

    return succeeded
}

fun File.mkPath() {
    if (!((this.exists() && this.isDirectory) || this.mkdirs()))
        throw IOException("Failed to create directory: ${this.absolutePath}")
}

@Throws(IOException::class)
fun InputStream.unzip(dstDir: File): String? {
    var fName: String? = null
    ZipInputStream(this).use { zis ->
        var ze = zis.nextEntry
        while (ze != null) {
            val newFile = File(dstDir, ze.name)
            if (ze.isDirectory) {
                fName ?: { fName = ze.name }
                newFile.mkPath()
            } else {
                newFile.parent?.let { File(it).mkPath() } ?: throw IOException("Failed to create parent directory")
                FileOutputStream(newFile).use { fos -> copyStream(zis, fos) }
            }
            ze = zis.nextEntry
        }
        zis.closeEntry()
    }
    return fName
}

private fun File.deleteRecursive() = (!this.exists()) || (this.deleteContents() && this.delete())

private fun copyStream(src: InputStream, dst: OutputStream) {
    val buffer = ByteArray(1024)
    while (true) {
        val n = src.read(buffer)
        if (n <= 0) break
        dst.write(buffer, 0, n)
    }
}
