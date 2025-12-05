package sh.bab.plugin.inspections

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLScalar
import sh.bab.plugin.BabBundle
import sh.bab.plugin.filetype.isBabfile
import sh.bab.plugin.util.BabPsiUtil

class BabUnresolvedTaskReferenceInspection : LocalInspectionTool() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        val file = holder.file
        if (file !is YAMLFile) return PsiElementVisitor.EMPTY_VISITOR

        val virtualFile = file.virtualFile ?: return PsiElementVisitor.EMPTY_VISITOR
        if (!isBabfile(virtualFile)) return PsiElementVisitor.EMPTY_VISITOR

        return object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                if (element !is YAMLScalar) return
                if (!BabPsiUtil.isTaskReferenceContext(element)) return

                val reference = element.textValue.trim()
                if (reference.isEmpty()) return

                val currentTaskName = BabPsiUtil.findCurrentTaskName(element)

                when {
                    reference == currentTaskName -> {
                        holder.registerProblem(
                            element,
                            BabBundle.message("inspection.self.dependency", reference),
                            ProblemHighlightType.ERROR
                        )
                    }
                    !BabPsiUtil.isValidTaskReference(file, reference) -> {
                        holder.registerProblem(
                            element,
                            BabBundle.message("inspection.unresolved.reference", reference),
                            ProblemHighlightType.ERROR
                        )
                    }
                }
            }
        }
    }
}