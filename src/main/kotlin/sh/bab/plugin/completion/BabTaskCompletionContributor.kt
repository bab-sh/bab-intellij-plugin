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

                    BabPsiUtil.extractTaskNames(yamlFile)
                        .filter { it != currentTaskName }
                        .forEach { taskName ->
                            result.addElement(
                                LookupElementBuilder.create(taskName)
                                    .withIcon(BabIcons.Task)
                                    .withTypeText("task", true)
                            )
                        }

                    BabPsiUtil.extractIncludeNames(yamlFile).forEach { includeName ->
                        result.addElement(
                            LookupElementBuilder.create("$includeName:")
                                .withIcon(BabIcons.Task)
                                .withTypeText("include", true)
                                .withTailText(" (external task)", true)
                        )
                    }
                }
            }
        )
    }
}
