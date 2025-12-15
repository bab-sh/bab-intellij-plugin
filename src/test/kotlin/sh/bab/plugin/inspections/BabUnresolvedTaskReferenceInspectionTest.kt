package sh.bab.plugin.inspections

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class BabUnresolvedTaskReferenceInspectionTest : BasePlatformTestCase() {

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(BabUnresolvedTaskReferenceInspection::class.java)
    }

    fun testValidTaskReference() {
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

        val highlights = myFixture.doHighlighting()
        val errors = highlights.filter { it.severity.name == "ERROR" }
        assertTrue("Expected no errors for valid task reference, but found: ${errors.map { it.description }}", errors.isEmpty())
    }

    fun testMultipleValidDependencies() {
        myFixture.configureByText("babfile.yml", """
            tasks:
              clean:
                run:
                  - cmd: echo clean
              compile:
                run:
                  - cmd: echo compile
              test:
                deps:
                  - clean
                  - compile
                run:
                  - cmd: echo test
        """.trimIndent())

        val highlights = myFixture.doHighlighting()
        val errors = highlights.filter { it.severity.name == "ERROR" }
        assertTrue("Expected no errors for valid dependencies", errors.isEmpty())
    }

    fun testNoInspectionOutsideDepsContext() {
        myFixture.configureByText("babfile.yml", """
            tasks:
              build:
                desc: This is a description with random text
                run:
                  - cmd: echo build
        """.trimIndent())

        val highlights = myFixture.doHighlighting()
        val errors = highlights.filter { it.severity.name == "ERROR" }
        assertTrue("Expected no errors outside deps context", errors.isEmpty())
    }

    fun testEmptyDepsArray() {
        myFixture.configureByText("babfile.yml", """
            tasks:
              standalone:
                deps:
                run:
                  - cmd: echo standalone
        """.trimIndent())

        val highlights = myFixture.doHighlighting()
        val errors = highlights.filter { it.severity.name == "ERROR" }
        assertTrue("Expected no errors for empty deps", errors.isEmpty())
    }

    fun testUnresolvedTaskReference() {
        myFixture.configureByText("babfile.yml", """
            tasks:
              test:
                deps:
                  - nonexistent
                run:
                  - cmd: echo test
        """.trimIndent())

        val highlights = myFixture.doHighlighting()
        val errors = highlights.filter { it.severity.name == "ERROR" }
        assertFalse("Expected error for unresolved task reference", errors.isEmpty())
        assertTrue("Expected unresolved reference error", errors.any { it.description?.contains("Unresolved") == true })
    }

    fun testSelfDependency() {
        myFixture.configureByText("babfile.yml", """
            tasks:
              circular:
                deps:
                  - circular
                run:
                  - cmd: echo circular
        """.trimIndent())

        val highlights = myFixture.doHighlighting()
        val errors = highlights.filter { it.severity.name == "ERROR" }
        assertFalse("Expected error for self-dependency", errors.isEmpty())
        assertTrue("Expected self-dependency error", errors.any {
            it.description?.contains("cannot depend on itself") == true ||
            it.description?.contains("circular") == true ||
            it.description?.contains("Unresolved") == true
        })
    }

    fun testMixedValidAndInvalidReferences() {
        myFixture.configureByText("babfile.yml", """
            tasks:
              build:
                run:
                  - cmd: echo build
              deploy:
                deps:
                  - build
                  - missing
                run:
                  - cmd: echo deploy
        """.trimIndent())

        val highlights = myFixture.doHighlighting()
        val errors = highlights.filter { it.severity.name == "ERROR" }
        assertFalse("Expected at least one error for missing task reference", errors.isEmpty())
    }
}
