package sh.bab.plugin.inspections

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLScalar
import sh.bab.plugin.filetype.BabFileType
import sh.bab.plugin.util.BabPsiUtil

class BabUnresolvedTaskReferenceInspection : LocalInspectionTool() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        val file = holder.file
        if (file !is YAMLFile) return PsiElementVisitor.EMPTY_VISITOR

        val virtualFile = file.virtualFile ?: return PsiElementVisitor.EMPTY_VISITOR
        if (!BabFileType.isBabfile(virtualFile)) return PsiElementVisitor.EMPTY_VISITOR

        val taskNames = BabPsiUtil.extractTaskNames(file)

        return object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                if (element !is YAMLScalar) return
                if (!BabPsiUtil.isInsideDepsField(element)) return

                val taskName = element.textValue.trim()
                if (taskName.isEmpty()) return

                val currentTaskName = BabPsiUtil.findCurrentTaskName(element)

                when {
                    taskName == currentTaskName -> holder.registerProblem(
                        element,
                        "Task '$taskName' cannot depend on itself",
                        ProblemHighlightType.ERROR
                    )
                    taskName !in taskNames -> holder.registerProblem(
                        element,
                        "Unresolved task reference '$taskName'",
                        ProblemHighlightType.ERROR
                    )
                }
            }
        }
    }
}
