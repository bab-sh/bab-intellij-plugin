package sh.bab.intellij.references

import com.intellij.openapi.util.TextRange
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.util.ProcessingContext
import org.jetbrains.yaml.psi.YAMLScalar
import sh.bab.intellij.filetype.BabFileType
import sh.bab.intellij.util.BabPsiUtil

class BabTaskReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(YAMLScalar::class.java),
            object : PsiReferenceProvider() {
                override fun getReferencesByElement(
                    element: PsiElement,
                    context: ProcessingContext
                ): Array<PsiReference> {
                    if (element !is YAMLScalar) return PsiReference.EMPTY_ARRAY

                    val file = element.containingFile?.virtualFile ?: return PsiReference.EMPTY_ARRAY
                    if (!BabFileType.isBabfile(file)) return PsiReference.EMPTY_ARRAY
                    if (!BabPsiUtil.isInsideDepsField(element)) return PsiReference.EMPTY_ARRAY

                    val taskName = element.textValue.trim()
                    if (taskName.isEmpty()) return PsiReference.EMPTY_ARRAY

                    return arrayOf(BabTaskReference(element, taskName, TextRange(0, element.textLength)))
                }
            }
        )
    }
}
