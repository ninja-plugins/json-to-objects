package com.ninja.jsontoobjects.util

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VirtualFile

object PackageDirectoryUtil {
    fun findOrCreatePackageDirectory(
        project: Project,
        module: Module?,
        packageName: String?
    ): VirtualFile? {
        val sourceRoots = when {
            module != null -> ModuleRootManager.getInstance(module).sourceRoots
            else -> ProjectRootManager.getInstance(project).contentSourceRoots
        }

        if (packageName.isNullOrBlank()) {
            return sourceRoots.firstOrNull() ?: project.guessProjectDir()
        }

        val packagePath = packageName.replace('.', '/')

        for (sourceRoot in sourceRoots) {
            val existingDir = sourceRoot.findFileByRelativePath(packagePath)
            if (existingDir != null && existingDir.isDirectory) {
                return existingDir
            }
        }

        val baseDir = sourceRoots.firstOrNull() ?: project.guessProjectDir() ?: return null

        return try {
            var currentDir = baseDir
            for (part in packageName.split('.')) {
                val existing = currentDir.findChild(part)
                currentDir = if (existing != null && existing.isDirectory) {
                    existing
                } else {
                    currentDir.createChildDirectory(PackageDirectoryUtil::class.java, part)
                }
            }
            currentDir
        } catch (e: Exception) {
            baseDir
        }
    }
}
