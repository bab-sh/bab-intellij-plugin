package sh.bab.plugin.completion

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns
import com.intellij.util.ProcessingContext
import org.jetbrains.yaml.psi.YAMLFile
import sh.bab.plugin.filetype.isBabfile
import sh.bab.plugin.icons.BabIcons
import sh.bab.plugin.util.BabPsiUtil

class BabTaskCompletionContributor : CompletionContributor() {
    init {
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement(),
            object : CompletionProvider<CompletionParameters>() {
                override fun addCompletions(
                    parameters: CompletionParameters,
                    context: ProcessingContext,
                    result: CompletionResultSet
                ) {
                    val file = parameters.originalFile.virtualFile ?: return
                    if (!isBabfile(file)) return
                    if (!BabPsiUtil.isTaskReferenceContext(parameters.position)) return

                    val yamlFile = parameters.originalFile as? YAMLFile ?: return
                    val currentTaskName = BabPsiUtil.findCurrentTaskName(parameters.position)
                    val currentTaskAliases = BabPsiUtil.findCurrentTaskAliases(parameters.position)

                    val includeNames = BabPsiUtil.extractIncludeNames(yamlFile)

                    BabPsiUtil.extractAllTaskReferencesWithType(yamlFile)
                        .filter { it.reference != currentTaskName && it.reference !in currentTaskAliases }
                        .forEach { taskRef ->
                            val prefix = taskRef.reference.substringBefore(":")
                            val isExternal = taskRef.reference.contains(":") && prefix in includeNames
                            val typeText = when {
                                taskRef.isAlias -> "alias"
                                isExternal -> BabPsiUtil.getIncludeBabfilePath(yamlFile, prefix) ?: prefix
                                else -> "task"
                            }
                            result.addElement(
                                LookupElementBuilder.create(taskRef.reference)
                                    .withIcon(BabIcons.Task)
                                    .withTypeText(typeText, true)
                            )
                        }
                }
            }
        )
    }
}
