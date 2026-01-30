package sh.bab.plugin.inspections

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLMapping
import org.jetbrains.yaml.psi.YAMLScalar
import org.jetbrains.yaml.psi.YAMLSequence
import sh.bab.plugin.BabBundle
import sh.bab.plugin.filetype.isBabfile
import sh.bab.plugin.util.YamlKeys

class BabDuplicateAliasInspection : LocalInspectionTool() {

    private data class AliasInfo(
        val alias: String,
        val taskName: String,
        val element: PsiElement
    )

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        val file = holder.file
        if (file !is YAMLFile) return PsiElementVisitor.EMPTY_VISITOR

        val virtualFile = file.virtualFile ?: return PsiElementVisitor.EMPTY_VISITOR
        if (!isBabfile(virtualFile)) return PsiElementVisitor.EMPTY_VISITOR

        return object : PsiElementVisitor() {
            private var analysisComplete = false

            override fun visitFile(file: PsiFile) {
                if (analysisComplete) return
                analysisComplete = true

                val yamlFile = file as? YAMLFile ?: return
                val tasksMapping = getTasksMapping(yamlFile) ?: return

                val taskNames = mutableSetOf<String>()
                val taskNameElements = mutableMapOf<String, PsiElement>()
                val allAliases = mutableListOf<AliasInfo>()

                for (taskKeyValue in tasksMapping.keyValues) {
                    val taskName = taskKeyValue.keyText
                    if (taskName.isEmpty()) continue

                    taskNames.add(taskName)
                    taskKeyValue.key?.let { taskNameElements[taskName] = it }

                    val taskMapping = taskKeyValue.value as? YAMLMapping ?: continue

                    val aliasKeyValue = taskMapping.keyValues.find { it.keyText == YamlKeys.ALIAS }
                    val aliasValue = aliasKeyValue?.value as? YAMLScalar
                    val alias = aliasValue?.textValue
                    if (!alias.isNullOrEmpty()) {
                        allAliases.add(AliasInfo(alias, taskName, aliasValue))
                    }

                    val aliasesSequence = taskMapping.keyValues.find { it.keyText == YamlKeys.ALIASES }?.value as? YAMLSequence
                    aliasesSequence?.items?.forEach { item ->
                        val aliasScalar = item.value as? YAMLScalar ?: return@forEach
                        val a = aliasScalar.textValue
                        if (a.isNotEmpty()) {
                            allAliases.add(AliasInfo(a, taskName, aliasScalar))
                        }
                    }
                }

                for (aliasInfo in allAliases) {
                    if (aliasInfo.alias in taskNames) {
                        holder.registerProblem(
                            aliasInfo.element,
                            BabBundle.message("inspection.alias.conflicts.task", aliasInfo.alias, aliasInfo.alias),
                            ProblemHighlightType.ERROR
                        )
                        taskNameElements[aliasInfo.alias]?.let { taskElement ->
                            holder.registerProblem(
                                taskElement,
                                BabBundle.message("inspection.alias.conflicts.task", aliasInfo.alias, aliasInfo.alias),
                                ProblemHighlightType.ERROR
                            )
                        }
                    }
                }

                val aliasesByName = allAliases.groupBy { it.alias }
                for ((alias, infos) in aliasesByName) {
                    if (infos.size > 1) {
                        val distinctTasks = infos.map { it.taskName }.distinct()
                        if (distinctTasks.size == 1) {
                            for (info in infos) {
                                holder.registerProblem(
                                    info.element,
                                    BabBundle.message("inspection.alias.duplicate", alias),
                                    ProblemHighlightType.ERROR
                                )
                            }
                        } else {
                            for (info in infos) {
                                val otherTasks = infos.filter { it.taskName != info.taskName }.map { it.taskName }.distinct().joinToString(", ")
                                holder.registerProblem(
                                    info.element,
                                    BabBundle.message("inspection.alias.conflicts.alias", alias, otherTasks),
                                    ProblemHighlightType.ERROR
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun getTasksMapping(file: YAMLFile): YAMLMapping? {
        val rootMapping = file.documents.firstOrNull()?.topLevelValue as? YAMLMapping ?: return null
        val tasksKeyValue = rootMapping.keyValues.find { it.keyText == YamlKeys.TASKS } ?: return null
        return tasksKeyValue.value as? YAMLMapping
    }
}
