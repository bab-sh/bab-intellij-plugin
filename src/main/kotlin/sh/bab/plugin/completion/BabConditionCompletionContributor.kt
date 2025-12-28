package sh.bab.plugin.completion

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns
import com.intellij.util.ProcessingContext
import sh.bab.plugin.filetype.isBabfile
import sh.bab.plugin.icons.BabIcons
import sh.bab.plugin.util.BabPsiUtil

class BabConditionCompletionContributor : CompletionContributor() {
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
                    if (!BabPsiUtil.isConditionValueContext(parameters.position)) return

                    result.addElement(
                        LookupElementBuilder.create("\${{  }}")
                            .withPresentableText("\${{ var }}")
                            .withIcon(BabIcons.FileType)
                            .withTypeText("truthy check", true)
                            .withTailText(" - runs if variable is non-empty", true)
                    )

                    result.addElement(
                        LookupElementBuilder.create("\${{  }} == ''")
                            .withPresentableText("\${{ var }} == 'value'")
                            .withIcon(BabIcons.FileType)
                            .withTypeText("equality", true)
                            .withTailText(" - runs if variable equals value", true)
                    )

                    result.addElement(
                        LookupElementBuilder.create("\${{  }} != ''")
                            .withPresentableText("\${{ var }} != 'value'")
                            .withIcon(BabIcons.FileType)
                            .withTypeText("inequality", true)
                            .withTailText(" - runs if variable differs", true)
                    )
                }
            }
        )
    }
}
