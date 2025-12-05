package sh.bab.plugin.references

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.ResolveResult
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLScalar
import sh.bab.plugin.util.BabPsiUtil

class BabTaskReference(
    element: YAMLScalar,
    private val taskName: String,
    textRange: TextRange
) : PsiReferenceBase<YAMLScalar>(element, textRange), PsiPolyVariantReference {

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        val yamlFile = element.containingFile as? YAMLFile ?: return ResolveResult.EMPTY_ARRAY
        val ref = BabPsiUtil.parseTaskReference(taskName)

        return if (ref.includePrefix != null) {
            BabPsiUtil.getIncludesMapping(yamlFile)
                ?.keyValues
                ?.filter { it.keyText == ref.includePrefix }
                ?.mapNotNull { it.key }
                ?.map { PsiElementResolveResult(it) }
                ?.toTypedArray()
                ?: ResolveResult.EMPTY_ARRAY
        } else {
            BabPsiUtil.getTaskKeyValues(yamlFile)
                .filter { it.keyText == ref.taskName }
                .mapNotNull { it.key }
                .map { PsiElementResolveResult(it) }
                .toTypedArray()
        }
    }

    override fun resolve(): PsiElement? = multiResolve(false).firstOrNull()?.element

    override fun getVariants(): Array<Any> = emptyArray()
}
