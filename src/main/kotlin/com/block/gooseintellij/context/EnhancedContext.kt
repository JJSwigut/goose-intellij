package com.block.gooseintellij.context

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import java.io.File

/**
 * Enhanced context information for better AI responses
 */
data class EnhancedContext(
    val selectedText: String? = null,
    val filePath: String? = null,
    val surroundingCode: String? = null,
    val imports: List<String> = emptyList(),
    val classContext: String? = null,
    val methodContext: String? = null,
    val projectType: String? = null,
    val buildSystem: String? = null
)

/**
 * Enhanced context collection utilities
 */
object EnhancedContextCollector {
    
    /**
     * Collect enhanced context from an action event
     */
    fun enhanceContextCollection(event: AnActionEvent): EnhancedContext {
        val project = event.project
        val editor = event.getData(CommonDataKeys.EDITOR)
        val psiFile = event.getData(CommonDataKeys.PSI_FILE)
        val virtualFile = event.getData(CommonDataKeys.VIRTUAL_FILE)
        
        return EnhancedContext(
            selectedText = getSelectedText(event),
            filePath = getFilePath(event),
            surroundingCode = getSurroundingCode(event, 10),
            imports = getFileImports(psiFile),
            classContext = getCurrentClass(event),
            methodContext = getCurrentMethod(event),
            projectType = project?.let { detectProjectType(it) },
            buildSystem = project?.let { detectBuildSystem(it) }
        )
    }
    
    /**
     * Get selected text from editor
     */
    private fun getSelectedText(event: AnActionEvent): String? {
        val editor = event.getData(CommonDataKeys.EDITOR)
        return editor?.selectionModel?.selectedText
    }
    
    /**
     * Get current file path
     */
    private fun getFilePath(event: AnActionEvent): String? {
        val virtualFile = event.getData(CommonDataKeys.VIRTUAL_FILE)
        return virtualFile?.path
    }
    
    /**
     * Get surrounding code context (lines before/after current position)
     */
    private fun getSurroundingCode(event: AnActionEvent, contextLines: Int): String? {
        return try {
            val editor = event.getData(CommonDataKeys.EDITOR) ?: return null
            val document = editor.document
            val caretOffset = editor.caretModel.offset
            val currentLine = document.getLineNumber(caretOffset)
            
            val startLine = maxOf(0, currentLine - contextLines)
            val endLine = minOf(document.lineCount - 1, currentLine + contextLines)
            
            val startOffset = document.getLineStartOffset(startLine)
            val endOffset = document.getLineEndOffset(endLine)
            
            document.getText(com.intellij.openapi.util.TextRange(startOffset, endOffset))
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Extract import statements from the file
     */
    private fun getFileImports(psiFile: PsiFile?): List<String> {
        return try {
            psiFile?.text?.lines()
                ?.filter { it.trim().startsWith("import ") }
                ?.map { it.trim() }
                ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Get current class context
     */
    private fun getCurrentClass(event: AnActionEvent): String? {
        return try {
            val psiFile = event.getData(CommonDataKeys.PSI_FILE) ?: return null
            val editor = event.getData(CommonDataKeys.EDITOR) ?: return null
            val offset = editor.caretModel.offset
            
            val element = psiFile.findElementAt(offset)
            var parent = element?.parent
            
            while (parent != null) {
                val text = parent.toString()
                if (text.contains("CLASS") || text.contains("OBJECT") || text.contains("INTERFACE")) {
                    // Extract class name from the first line
                    val firstLine = parent.text.lines().firstOrNull()?.trim()
                    if (firstLine != null && (firstLine.contains("class ") || firstLine.contains("object ") || firstLine.contains("interface "))) {
                        return firstLine
                    }
                }
                parent = parent.parent
            }
            null
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Get current method context
     */
    private fun getCurrentMethod(event: AnActionEvent): String? {
        return try {
            val psiFile = event.getData(CommonDataKeys.PSI_FILE) ?: return null
            val editor = event.getData(CommonDataKeys.EDITOR) ?: return null
            val offset = editor.caretModel.offset
            
            val element = psiFile.findElementAt(offset)
            var parent = element?.parent
            
            while (parent != null) {
                val text = parent.toString()
                if (text.contains("METHOD") || text.contains("FUNCTION")) {
                    // Extract method signature from the first line
                    val firstLine = parent.text.lines().firstOrNull()?.trim()
                    if (firstLine != null && (firstLine.contains("fun ") || firstLine.contains("def ") || firstLine.contains("function "))) {
                        return firstLine
                    }
                }
                parent = parent.parent
            }
            null
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Detect project type based on files and structure
     */
    private fun detectProjectType(project: Project): String {
        val basePath = project.basePath ?: return "unknown"
        val projectDir = File(basePath)
        
        return when {
            projectDir.resolve("build.gradle.kts").exists() || 
            projectDir.resolve("build.gradle").exists() -> "gradle"
            projectDir.resolve("pom.xml").exists() -> "maven"
            projectDir.resolve("package.json").exists() -> "npm"
            projectDir.resolve("Cargo.toml").exists() -> "cargo"
            projectDir.resolve("go.mod").exists() -> "go-module"
            projectDir.resolve("requirements.txt").exists() || 
            projectDir.resolve("setup.py").exists() -> "python"
            else -> "unknown"
        }
    }
    
    /**
     * Detect build system
     */
    private fun detectBuildSystem(project: Project): String {
        val basePath = project.basePath ?: return "unknown"
        val projectDir = File(basePath)
        
        return when {
            projectDir.resolve("build.gradle.kts").exists() -> "gradle-kotlin"
            projectDir.resolve("build.gradle").exists() -> "gradle"
            projectDir.resolve("pom.xml").exists() -> "maven"
            projectDir.resolve("package.json").exists() -> "npm"
            projectDir.resolve("yarn.lock").exists() -> "yarn"
            projectDir.resolve("Cargo.toml").exists() -> "cargo"
            projectDir.resolve("Makefile").exists() -> "make"
            else -> "unknown"
        }
    }
}