package sh.bab.plugin.util

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.yaml.psi.YAMLFile
import sh.bab.plugin.model.YamlKeys

class BabPsiUtilTest : BasePlatformTestCase() {

    fun testIsTaskReferenceContextInDeps() {
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
        assertNotNull("Element should exist at caret", element)
        assertTrue("Should be in task reference context (deps)", BabContextUtil.isTaskReferenceContext(element!!))
    }

    fun testIsTaskReferenceContextInRunTask() {
        myFixture.configureByText("babfile.yml", """
            tasks:
              helper:
                run:
                  - cmd: echo helper
              main:
                run:
                  - task: help<caret>er
        """.trimIndent())

        val element = myFixture.file.findElementAt(myFixture.caretOffset)
        assertNotNull("Element should exist at caret", element)
        assertTrue("Should be in task reference context (run.task)", BabContextUtil.isTaskReferenceContext(element!!))
    }

    fun testIsTaskReferenceContextOutsideReturnsNull() {
        myFixture.configureByText("babfile.yml", """
            tasks:
              build:
                desc: This is a descr<caret>iption
                run:
                  - cmd: echo build
        """.trimIndent())

        val element = myFixture.file.findElementAt(myFixture.caretOffset)
        assertNotNull("Element should exist at caret", element)
        assertFalse("Should NOT be in task reference context (desc)", BabContextUtil.isTaskReferenceContext(element!!))
    }

    fun testIsTaskReferenceContextInCmdField() {
        myFixture.configureByText("babfile.yml", """
            tasks:
              build:
                run:
                  - cmd: echo bui<caret>ld
        """.trimIndent())

        val element = myFixture.file.findElementAt(myFixture.caretOffset)
        assertNotNull("Element should exist at caret", element)
        assertFalse("Should NOT be in task reference context (cmd)", BabContextUtil.isTaskReferenceContext(element!!))
    }

    fun testIsIncludeBabfileContext() {
        myFixture.configureByText("babfile.yml", """
            includes:
              utils:
                babfile: ./utils/babf<caret>ile.yml
            tasks:
              main:
                run:
                  - cmd: echo main
        """.trimIndent())

        val element = myFixture.file.findElementAt(myFixture.caretOffset)
        assertNotNull("Element should exist at caret", element)
        assertTrue("Should be in include babfile context", BabContextUtil.isIncludeBabfileContext(element!!))
    }

    fun testIsIncludeBabfileContextOutside() {
        myFixture.configureByText("babfile.yml", """
            includes:
              uti<caret>ls:
                babfile: ./utils/babfile.yml
            tasks:
              main:
                run:
                  - cmd: echo main
        """.trimIndent())

        val element = myFixture.file.findElementAt(myFixture.caretOffset)
        assertNotNull("Element should exist at caret", element)
        assertFalse("Should NOT be in include babfile context (prefix key)", BabContextUtil.isIncludeBabfileContext(element!!))
    }

    fun testFindCurrentTaskName() {
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
        assertNotNull("Element should exist at caret", element)

        val taskName = BabTaskUtil.findCurrentTaskName(element!!)
        assertEquals("Current task should be 'test'", "test", taskName)
    }

    fun testFindCurrentTaskNameInRun() {
        myFixture.configureByText("babfile.yml", """
            tasks:
              deploy:
                run:
                  - cmd: echo depl<caret>oy
        """.trimIndent())

        val element = myFixture.file.findElementAt(myFixture.caretOffset)
        assertNotNull("Element should exist at caret", element)

        val taskName = BabTaskUtil.findCurrentTaskName(element!!)
        assertEquals("Current task should be 'deploy'", "deploy", taskName)
    }

    fun testExtractIncludeNames() {
        val psiFile = myFixture.configureByText("babfile.yml", """
            includes:
              utils:
                babfile: ./utils/babfile.yml
              libs:
                babfile: ./libs/babfile.yml
              common:
                babfile: ./common/babfile.yml
            tasks:
              main:
                run:
                  - cmd: echo main
        """.trimIndent()) as YAMLFile

        val includeNames = BabTaskUtil.extractIncludeNames(psiFile)
        assertEquals("Should have 3 includes", 3, includeNames.size)
        assertTrue("Should contain 'utils'", includeNames.contains("utils"))
        assertTrue("Should contain 'libs'", includeNames.contains("libs"))
        assertTrue("Should contain 'common'", includeNames.contains("common"))
    }

    fun testExtractIncludeNamesEmpty() {
        val psiFile = myFixture.configureByText("babfile.yml", """
            tasks:
              main:
                run:
                  - cmd: echo main
        """.trimIndent()) as YAMLFile

        val includeNames = BabTaskUtil.extractIncludeNames(psiFile)
        assertTrue("Should have no includes", includeNames.isEmpty())
    }

    fun testGetIncludeBabfilePath() {
        val psiFile = myFixture.configureByText("babfile.yml", """
            includes:
              utils:
                babfile: ./utils/babfile.yml
            tasks:
              main:
                run:
                  - cmd: echo main
        """.trimIndent()) as YAMLFile

        val path = BabTaskUtil.getIncludeBabfilePath(psiFile, "utils")
        assertEquals("./utils/babfile.yml", path)
    }

    fun testGetIncludeBabfilePathNotFound() {
        val psiFile = myFixture.configureByText("babfile.yml", """
            includes:
              utils:
                babfile: ./utils/babfile.yml
            tasks:
              main:
                run:
                  - cmd: echo main
        """.trimIndent()) as YAMLFile

        val path = BabTaskUtil.getIncludeBabfilePath(psiFile, "nonexistent")
        assertNull("Should return null for non-existent include", path)
    }

    fun testExtractAllTaskReferences() {
        val psiFile = myFixture.configureByText("babfile.yml", """
            tasks:
              build:
                run:
                  - cmd: echo build
              test:
                run:
                  - cmd: echo test
              deploy:
                run:
                  - cmd: echo deploy
        """.trimIndent()) as YAMLFile

        val tasks = BabTaskUtil.extractAllTaskReferences(psiFile)
        assertEquals("Should have 3 tasks", 3, tasks.size)
        assertTrue("Should contain 'build'", tasks.contains("build"))
        assertTrue("Should contain 'test'", tasks.contains("test"))
        assertTrue("Should contain 'deploy'", tasks.contains("deploy"))
    }

    fun testResolveTaskReferenceLocal() {
        val psiFile = myFixture.configureByText("babfile.yml", """
            tasks:
              build:
                run:
                  - cmd: echo build
              test:
                deps:
                  - build
                run:
                  - cmd: echo test
        """.trimIndent()) as YAMLFile

        val resolved = BabTaskUtil.resolveTaskReference(psiFile, "build")
        assertNotNull("Should resolve 'build' task", resolved)
        assertEquals("Resolved key should be 'build'", "build", resolved?.keyText)
    }

    fun testResolveTaskReferenceNotFound() {
        val psiFile = myFixture.configureByText("babfile.yml", """
            tasks:
              build:
                run:
                  - cmd: echo build
        """.trimIndent()) as YAMLFile

        val resolved = BabTaskUtil.resolveTaskReference(psiFile, "nonexistent")
        assertNull("Should return null for non-existent task", resolved)
    }

    fun testIsValidTaskReference() {
        val psiFile = myFixture.configureByText("babfile.yml", """
            tasks:
              build:
                run:
                  - cmd: echo build
              test:
                run:
                  - cmd: echo test
        """.trimIndent()) as YAMLFile

        assertTrue("'build' should be valid", BabTaskUtil.isValidTaskReference(psiFile, "build"))
        assertTrue("'test' should be valid", BabTaskUtil.isValidTaskReference(psiFile, "test"))
        assertFalse("'nonexistent' should be invalid", BabTaskUtil.isValidTaskReference(psiFile, "nonexistent"))
    }

    fun testYamlKeysConstants() {
        assertEquals("TASKS constant", "tasks", YamlKeys.TASKS)
        assertEquals("DEPS constant", "deps", YamlKeys.DEPS)
        assertEquals("RUN constant", "run", YamlKeys.RUN)
        assertEquals("INCLUDES constant", "includes", YamlKeys.INCLUDES)
        assertEquals("BABFILE constant", "babfile", YamlKeys.BABFILE)
        assertEquals("DESC constant", "desc", YamlKeys.DESC)
    }
}
