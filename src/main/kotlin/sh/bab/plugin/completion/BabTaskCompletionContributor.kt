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

                    val includeNames = BabPsiUtil.extractIncludeNames(yamlFile)

                    BabPsiUtil.extractAllTaskReferences(yamlFile)
                        .filter { it != currentTaskName }
                        .forEach { taskRef ->
                            val prefix = taskRef.substringBefore(":")
                            val isExternal = taskRef.contains(":") && prefix in includeNames
                            val typeText = if (isExternal) {
                                BabPsiUtil.getIncludeBabfilePath(yamlFile, prefix) ?: prefix
                            } else {
                                "task"
                            }
                            result.addElement(
                                LookupElementBuilder.create(taskRef)
                                    .withIcon(BabIcons.Task)
                                    .withTypeText(typeText, true)
                            )
                        }
                }
            }
        )
    }
}
