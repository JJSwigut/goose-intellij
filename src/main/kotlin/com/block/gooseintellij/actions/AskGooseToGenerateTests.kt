package com.block.gooseintellij.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.diagnostic.Logger

const val GOOSE_TEST_COMMAND_FORMAT = "Analyze the current repo for the correct location of unit tests. Generate tests for the following code. " +
  "If the test file for the code already exists, add the tests to the existing file."

class AskGooseToGenerateTestsAction : AnAction() {

    private val logger = Logger.getInstance(AskGooseToGenerateTestsAction::class.java)

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return

        val editor = event.getData(CommonDataKeys.EDITOR)
        val selectedText = editor?.selectionModel?.selectedText
        val psiFile = event.getData(CommonDataKeys.PSI_FILE)

        if (selectedText != null && selectedText.isNotEmpty() && psiFile != null) {
            val filePath = psiFile.virtualFile.path
            GooseActionHelper.checkAndSendToGoose(event, GOOSE_TEST_COMMAND_FORMAT) {
                Triple(selectedText, filePath, true)
            }
        } else {
            Messages.showMessageDialog("No file or text selected.", "Warning", Messages.getWarningIcon())
        }
    }
}