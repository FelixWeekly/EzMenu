package com.ezmenu.app.util

import java.io.File

/** 持久化目录无关引用，同时兼容备份恢复前保存的 /data/.../files/covers 路径。 */
object CoverReferences {
    fun resolve(filesDir: File, ref: String?): File? {
        if (ref == null) return null
        val covers = File(filesDir, "covers")
        val path = ref.removePrefix("file://").replace('\\', '/')
        val name = when {
            ref.startsWith("cover:") -> ref.removePrefix("cover:")
            path.contains("/files/covers/") -> path.substringAfterLast('/')
            else -> return File(path).takeIf { it.isAbsolute && it.isFile }
        }
        if (name.isBlank() || name == "." || name == ".." || name.contains('/') || name.contains('\\')) return null
        return File(covers, name).takeIf { it.isFile }
    }
}
