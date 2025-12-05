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

        val taskNames = BabPsiUtil.extractTaskNames(file)
        val includeNames = BabPsiUtil.extractIncludeNames(file)

        return object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                if (element !is YAMLScalar) return
                if (!BabPsiUtil.isTaskReferenceContext(element)) return

                val rawReference = element.textValue.trim()
                if (rawReference.isEmpty()) return

                val ref = BabPsiUtil.parseTaskReference(rawReference)
                val currentTaskName = BabPsiUtil.findCurrentTaskName(element)

                when {
                    ref.includePrefix == null && ref.taskName == currentTaskName -> {
                        holder.registerProblem(
                            element,
                            BabBundle.message("inspection.self.dependency", ref.taskName),
                            ProblemHighlightType.ERROR
                        )
                    }
                    ref.includePrefix != null -> {
                        if (ref.includePrefix !in includeNames) {
                            holder.registerProblem(
                                element,
                                BabBundle.message("inspection.unresolved.include", ref.includePrefix),
                                ProblemHighlightType.ERROR
                            )
                        }
                    }
                    ref.taskName !in taskNames -> {
                        holder.registerProblem(
                            element,
                            BabBundle.message("inspection.unresolved.reference", ref.taskName),
                            ProblemHighlightType.ERROR
                        )
                    }
                }
            }
        }
    }
}
