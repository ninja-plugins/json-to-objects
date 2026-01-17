package com.ninja.jsontoobjects.action

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import com.ninja.jsontoobjects.dialog.ConvertOptionsDialog
import com.ninja.jsontoobjects.generator.JavaGenerator
import com.ninja.jsontoobjects.generator.KotlinGenerator
import com.ninja.jsontoobjects.model.TargetLanguage
import com.ninja.jsontoobjects.parser.JsonParser
import com.ninja.jsontoobjects.util.StringUtils

class ConvertJsonAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun update(e: AnActionEvent) {
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)
        val editor = e.getData(CommonDataKeys.EDITOR)

        // JSON 파일이거나, 에디터에서 텍스트가 선택되어 있으면 활성화
        val isJsonFile = file != null && file.extension?.lowercase() == "json"
        val hasSelection = editor != null && editor.selectionModel.hasSelection()

        e.presentation.isEnabledAndVisible = isJsonFile || hasSelection
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)
        val editor = e.getData(CommonDataKeys.EDITOR)

        // JSON 내용 가져오기
        val (jsonContent, suggestedClassName, targetDir) = when {
            // 1. 에디터에서 선택된 텍스트
            editor != null && editor.selectionModel.hasSelection() -> {
                val selectedText = editor.selectionModel.selectedText ?: ""
                Triple(selectedText, "Generated", file?.parent)
            }
            // 2. JSON 파일
            file != null && file.extension?.lowercase() == "json" -> {
                val content = try {
                    String(file.contentsToByteArray(), Charsets.UTF_8)
                } catch (ex: Exception) {
                    Messages.showErrorDialog(project, "Failed to read file: ${ex.message}", "Error")
                    return
                }
                Triple(content, StringUtils.toPascalCase(file.nameWithoutExtension), file.parent)
            }

            else -> {
                Triple("", "Generated", null)
            }
        }

        processConversion(project, jsonContent, suggestedClassName, targetDir)
    }

    companion object {
        fun processConversion(
            project: Project,
            initialJson: String,
            suggestedClassName: String,
            targetDir: VirtualFile?
        ) {
            // Show options dialog
            val dialog = ConvertOptionsDialog(project, suggestedClassName, initialJson)
            if (!dialog.showAndGet()) {
                return
            }

            val options = dialog.getOptions()
            val jsonContent = dialog.getJsonInput()

            if (jsonContent.isBlank()) {
                Messages.showErrorDialog(project, "JSON input is empty", "Error")
                return
            }

            // Parse JSON
            val parser = JsonParser()
            val parseResult = try {
                parser.parse(jsonContent, options.className)
            } catch (ex: Exception) {
                Messages.showErrorDialog(project, "Failed to parse JSON: ${ex.message}", "Parse Error")
                return
            }

            // Generate code
            val generatedFiles = when (options.targetLanguage) {
                TargetLanguage.JAVA -> {
                    val generator = JavaGenerator(options.javaOptions)
                    generator.generate(parseResult, options.className)
                }

                TargetLanguage.KOTLIN -> {
                    val generator = KotlinGenerator(options.kotlinOptions)
                    generator.generate(parseResult, options.className)
                }
            }

            // 타겟 디렉토리가 없으면 프로젝트 루트 사용
            val outputDir = targetDir ?: project.guessProjectDir()
            if (outputDir == null) {
                // 파일 저장 없이 클립보드에 복사
                val content = generatedFiles.values.joinToString("\n\n")
                val clipboard = java.awt.Toolkit.getDefaultToolkit().systemClipboard
                clipboard.setContents(java.awt.datatransfer.StringSelection(content), null)
                Messages.showInfoMessage(project, "Generated code copied to clipboard!", "Success")
                return
            }

            // Create files
            WriteCommandAction.runWriteCommandAction(project) {
                for ((fileName, content) in generatedFiles) {
                    createOrUpdateFile(outputDir, fileName, content)
                }
            }

            // Open the first generated file
            generatedFiles.keys.firstOrNull()?.let { fileName ->
                outputDir.findChild(fileName)?.let { createdFile ->
                    FileEditorManager.getInstance(project).openFile(createdFile, true)
                }
            }

            val fileCount = generatedFiles.size
            val message = if (fileCount == 1) {
                "Generated ${generatedFiles.keys.first()}"
            } else {
                "Generated $fileCount files"
            }
            Messages.showInfoMessage(project, message, "Success")
        }

        private fun createOrUpdateFile(parentDir: VirtualFile, fileName: String, content: String) {
            val existingFile = parentDir.findChild(fileName)
            if (existingFile != null) {
                existingFile.setBinaryContent(content.toByteArray(Charsets.UTF_8))
            } else {
                val newFile = parentDir.createChildData(this, fileName)
                newFile.setBinaryContent(content.toByteArray(Charsets.UTF_8))
            }
        }
    }
}
