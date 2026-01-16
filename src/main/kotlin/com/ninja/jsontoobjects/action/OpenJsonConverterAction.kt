package com.ninja.jsontoobjects.action

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys

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

        // 에디터에서 선택된 텍스트가 있으면 가져오기
        val selectedText = editor?.selectionModel?.selectedText ?: ""

        ConvertJsonAction.processConversion(
            project = project,
            initialJson = selectedText,
            suggestedClassName = "Generated",
            targetDir = file?.parent ?: project.baseDir
        )
    }
}
