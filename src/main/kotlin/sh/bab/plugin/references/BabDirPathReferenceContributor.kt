package sh.bab.plugin.references

import com.intellij.openapi.paths.PathReferenceManager
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.util.ProcessingContext
import org.jetbrains.yaml.psi.YAMLScalar
import sh.bab.plugin.filetype.isBabfile
import sh.bab.plugin.util.BabPsiUtil

class BabDirPathReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(YAMLScalar::class.java),
            object : PsiReferenceProvider() {
                override fun getReferencesByElement(
                    element: PsiElement,
                    context: ProcessingContext
                ): Array<PsiReference> {
                    if (element !is YAMLScalar) return PsiReference.EMPTY_ARRAY

                    val virtualFile = element.containingFile.originalFile.virtualFile
                        ?: return PsiReference.EMPTY_ARRAY
                    if (!isBabfile(virtualFile)) return PsiReference.EMPTY_ARRAY
                    if (!BabPsiUtil.isDirContext(element)) return PsiReference.EMPTY_ARRAY

                    if (element.textValue.isEmpty()) return PsiReference.EMPTY_ARRAY

                    return PathReferenceManager.getInstance()
                        .createReferences(element, false, false, true)
                }
            }
        )
    }
}
