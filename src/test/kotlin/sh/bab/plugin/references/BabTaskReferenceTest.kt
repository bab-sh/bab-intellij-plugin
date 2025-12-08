package sh.bab.plugin.references

import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.PsiReference
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.yaml.psi.YAMLKeyValue

class BabTaskReferenceTest : BasePlatformTestCase() {

    fun testElementAtCaretExists() {
        myFixture.configureByText("babfile.yml", """
            tasks:
              build:
                run:
                  - cmd: echo build
              test:
                deps:
                  - bui<caret>ld
                run:
                  - cmd: echo test
        """.trimIndent())

        val element = myFixture.file.findElementAt(myFixture.caretOffset)
        assertNotNull("Expected element at caret position", element)
    }

    fun testYAMLFileIsRecognized() {
        val psiFile = myFixture.configureByText("babfile.yml", """
            tasks:
              build:
                run:
                  - cmd: echo build
        """.trimIndent())

        assertNotNull("Expected PSI file to be created", psiFile)
        assertTrue("Expected YAML language file", psiFile.language.displayName.contains("YAML", ignoreCase = true))
    }

    fun testReferenceAtCaretInDepsField() {
        myFixture.configureByText("babfile.yml", """
            tasks:
              build:
                run:
                  - cmd: echo build
              test:
                deps:
                  - buil<caret>d
                run:
                  - cmd: echo test
        """.trimIndent())

        val reference = myFixture.getReferenceAtCaretPosition()
        assertNotNull("Expected reference at caret position in deps field", reference)
    }

    fun testReferenceResolvesToTargetTask() {
        myFixture.configureByText("babfile.yml", """
            tasks:
              build:
                run:
                  - cmd: echo build
              test:
                deps:
                  - buil<caret>d
                run:
                  - cmd: echo test
        """.trimIndent())

        val reference = myFixture.getReferenceAtCaretPosition()
        assertNotNull("Expected reference at caret", reference)

        val resolved = reference?.resolve()
        assertNotNull("Expected reference to resolve", resolved)
        assertTrue("Expected resolved element to be YAMLKeyValue or its key",
            resolved is YAMLKeyValue || resolved?.parent is YAMLKeyValue)
    }

    fun testReferenceMultiResolve() {
        myFixture.configureByText("babfile.yml", """
            tasks:
              build:
                run:
                  - cmd: echo build
              test:
                deps:
                  - buil<caret>d
                run:
                  - cmd: echo test
        """.trimIndent())

        val reference = myFixture.getReferenceAtCaretPosition()
        assertNotNull("Expected reference at caret", reference)

        if (reference is PsiPolyVariantReference) {
            val results = reference.multiResolve(false)
            assertTrue("Expected at least one resolve result", results.isNotEmpty())
            assertNotNull("Expected first result to have element", results[0].element)
        }
    }

    fun testUnresolvedReferenceReturnsNull() {
        myFixture.configureByText("babfile.yml", """
            tasks:
              test:
                deps:
                  - nonexist<caret>ent
                run:
                  - cmd: echo test
        """.trimIndent())

        val reference = myFixture.getReferenceAtCaretPosition()
        assertNotNull("Expected reference at caret", reference)

        val resolved = reference?.resolve()
        assertNull("Expected unresolved reference to return null", resolved)
    }

    fun testReferenceInRunTaskField() {
        myFixture.configureByText("babfile.yml", """
            tasks:
              helper:
                run:
                  - cmd: echo helper
              main:
                run:
                  - task: help<caret>er
        """.trimIndent())

        val reference = myFixture.getReferenceAtCaretPosition()
        assertNotNull("Expected reference in run.task field", reference)

        val resolved = reference?.resolve()
        assertNotNull("Expected run.task reference to resolve", resolved)
    }

    fun testNoReferenceOutsideTaskContext() {
        myFixture.configureByText("babfile.yml", """
            tasks:
              build:
                desc: This is a descr<caret>iption
                run:
                  - cmd: echo build
        """.trimIndent())

        val reference = myFixture.getReferenceAtCaretPosition()
        if (reference != null) {
            assertFalse("Should not be a BabTaskReference in desc field",
                reference is BabTaskReference)
        }
    }

    fun testDepsFieldStructure() {
        myFixture.configureByText("babfile.yml", """
            tasks:
              build:
                run:
                  - cmd: echo build
              test:
                deps:
                  - build
                run:
                  - cmd: echo test
        """.trimIndent())

        val text = myFixture.file.text
        assertTrue("File should contain tasks section", text.contains("tasks:"))
        assertTrue("File should contain deps section", text.contains("deps:"))
        assertTrue("File should contain build reference", text.contains("- build"))
    }

    fun testNamespacedTaskReference() {
        myFixture.configureByText("babfile.yml", """
            tasks:
              build:lint:
                run:
                  - cmd: echo lint
              build:compile:
                run:
                  - cmd: echo compile
              test:
                deps:
                  - build:li<caret>nt
                run:
                  - cmd: echo test
        """.trimIndent())

        val reference = myFixture.getReferenceAtCaretPosition()
        assertNotNull("Expected reference for namespaced task", reference)

        val resolved = reference?.resolve()
        assertNotNull("Expected namespaced task reference to resolve", resolved)
    }
}
