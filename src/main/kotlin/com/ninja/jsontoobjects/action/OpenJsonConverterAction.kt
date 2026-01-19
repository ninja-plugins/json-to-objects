package com.ninja.jsontoobjects.action

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VirtualFile
import com.ninja.jsontoobjects.util.PackageExtractor

class OpenJsonConverterAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)
        val editor = e.getData(CommonDataKeys.EDITOR)

        // 모듈 정보 가져오기
        val module = file?.let { ModuleUtilCore.findModuleForFile(it, project) }

        // 에디터에서 선택된 텍스트가 있으면 가져오기
        val selectedText = editor?.selectionModel?.selectedText ?: ""

        // 현재 열린 파일에서 패키지 감지
        val suggestedPackage = detectPackageFromCurrentFile(project, file)

        ConvertJsonAction.processConversion(
            project = project,
            module = module,
            initialJson = selectedText,
            suggestedClassName = "Generated",
            targetDir = file?.parent ?: project.guessProjectDir(),
            suggestedPackage = suggestedPackage
        )
    }

    private fun detectPackageFromCurrentFile(project: Project, file: VirtualFile?): String? {
        // 1. 현재 파일에서 패키지 찾기
        if (file != null) {
            extractPackageFromFile(file)?.let { return it }
        }

        // 2. 열려있는 파일들에서 찾기
        val fileEditorManager = FileEditorManager.getInstance(project)

        val selectedFile = fileEditorManager.selectedFiles.firstOrNull()
        if (selectedFile != null && selectedFile != file) {
            extractPackageFromFile(selectedFile)?.let { return it }
        }

        for (openFile in fileEditorManager.openFiles) {
            if (openFile == file || openFile == selectedFile) continue
            extractPackageFromFile(openFile)?.let { return it }
        }

        return null
    }

    private fun extractPackageFromFile(file: VirtualFile): String? {
        if (!PackageExtractor.isSourceFile(file.extension)) return null
        return try {
            val content = String(file.contentsToByteArray(), Charsets.UTF_8)
            PackageExtractor.extractPackage(content)
        } catch (e: Exception) {
            null
        }
    }
}
