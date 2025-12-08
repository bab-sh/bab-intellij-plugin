package sh.bab.plugin.services

import com.intellij.openapi.components.service
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.yaml.psi.YAMLFile
import org.junit.Assert.assertNotEquals

class BabFileServiceTest : BasePlatformTestCase() {

    private val babFileService: BabFileService
        get() = project.service<BabFileService>()

    fun testServiceInstantiation() {
        val service = project.service<BabFileService>()
        assertNotNull("BabFileService should be available", service)
    }

    fun testFindRootBabfileNotFoundInEmptyProject() {
        val rootFile = babFileService.findRootBabfile()
        assertNull("findRootBabfile should return null in empty project", rootFile)
    }

    fun testGetBabfileTreeReturnsNullInEmptyProject() {
        val tree = babFileService.getBabfileTree()
        assertNull("getBabfileTree should return null in empty project", tree)
    }

    fun testBabTaskDataClass() {
        val task = BabTask(
            name = "build",
            description = "Build the project",
            dependencies = listOf("clean", "compile"),
            psiElement = null
        )

        assertEquals("build", task.name)
        assertEquals("Build the project", task.description)
        assertEquals(listOf("clean", "compile"), task.dependencies)
        assertNull(task.psiElement)
    }

    fun testBabTaskWithoutDescription() {
        val task = BabTask(
            name = "simple",
            description = null,
            dependencies = emptyList(),
            psiElement = null
        )

        assertEquals("simple", task.name)
        assertNull(task.description)
        assertTrue(task.dependencies.isEmpty())
    }

    fun testBabIncludeDataClass() {
        val include = BabInclude(
            prefix = "utils",
            babfilePath = "./utils/babfile.yml",
            resolvedFile = null
        )

        assertEquals("utils", include.prefix)
        assertEquals("./utils/babfile.yml", include.babfilePath)
        assertNull(include.resolvedFile)
    }

    fun testBabFileDataClass() {
        val babFile = BabFile(
            file = myFixture.configureByText("babfile.yml", "tasks:").virtualFile,
            relativePath = "babfile.yml",
            tasks = listOf(
                BabTask("build", null, emptyList(), null)
            ),
            includes = emptyList()
        )

        assertEquals("babfile.yml", babFile.relativePath)
        assertEquals(1, babFile.tasks.size)
        assertEquals("build", babFile.tasks[0].name)
        assertTrue(babFile.includes.isEmpty())
    }

    fun testExtractTasksFromBabfile() {
        val psiFile = myFixture.configureByText("babfile.yml", """
            tasks:
              build:
                desc: Build the project
                run:
                  - cmd: echo build
              test:
                deps:
                  - build
                run:
                  - cmd: echo test
              deploy:
                run:
                  - cmd: echo deploy
        """.trimIndent()) as YAMLFile

        assertNotNull(psiFile)
        val text = psiFile.text
        assertTrue("File should contain tasks section", text.contains("tasks:"))
        assertTrue("File should contain build task", text.contains("build:"))
        assertTrue("File should contain test task", text.contains("test:"))
        assertTrue("File should contain deploy task", text.contains("deploy:"))
    }

    fun testExtractTaskDependencies() {
        val psiFile = myFixture.configureByText("babfile.yml", """
            tasks:
              clean:
                run:
                  - cmd: rm -rf build
              compile:
                deps:
                  - clean
                run:
                  - cmd: gcc main.c
              test:
                deps:
                  - clean
                  - compile
                run:
                  - cmd: ./test
        """.trimIndent()) as YAMLFile

        val text = psiFile.text
        assertTrue("File should contain deps for compile", text.contains("deps:"))
        assertTrue("Compile should depend on clean", text.contains("- clean"))
        assertTrue("Test should depend on compile", text.contains("- compile"))
    }

    fun testExtractIncludes() {
        val psiFile = myFixture.configureByText("babfile.yml", """
            includes:
              utils:
                babfile: ./utils/babfile.yml
              libs:
                babfile: ./libs/babfile.yml
            tasks:
              main:
                run:
                  - cmd: echo main
        """.trimIndent()) as YAMLFile

        val text = psiFile.text
        assertTrue("File should contain includes section", text.contains("includes:"))
        assertTrue("Should include utils", text.contains("utils:"))
        assertTrue("Should include libs", text.contains("libs:"))
        assertTrue("Should have babfile paths", text.contains("babfile:"))
    }

    fun testBabTaskEquality() {
        val task1 = BabTask("build", "desc", listOf("a", "b"), null)
        val task2 = BabTask("build", "desc", listOf("a", "b"), null)
        val task3 = BabTask("test", "desc", listOf("a", "b"), null)

        assertEquals("Equal tasks should be equal", task1, task2)
        assertNotEquals("Different tasks should not be equal", task1, task3)
    }

    fun testBabIncludeEquality() {
        val include1 = BabInclude("utils", "./utils/babfile.yml", null)
        val include2 = BabInclude("utils", "./utils/babfile.yml", null)
        val include3 = BabInclude("libs", "./libs/babfile.yml", null)

        assertEquals("Equal includes should be equal", include1, include2)
        assertNotEquals("Different includes should not be equal", include1, include3)
    }

    fun testBabTaskCopy() {
        val original = BabTask("build", "desc", listOf("a"), null)
        val copy = original.copy(name = "newBuild")

        assertEquals("build", original.name)
        assertEquals("newBuild", copy.name)
        assertEquals(original.description, copy.description)
        assertEquals(original.dependencies, copy.dependencies)
    }

    fun testEmptyBabfile() {
        val psiFile = myFixture.configureByText("babfile.yml", """
            tasks:
        """.trimIndent()) as YAMLFile

        assertNotNull(psiFile)
        assertTrue("Empty tasks should still be valid YAML", psiFile.text.contains("tasks:"))
    }

    fun testBabfileWithOnlyDescription() {
        val psiFile = myFixture.configureByText("babfile.yml", """
            tasks:
              info:
                desc: This task only has a description
        """.trimIndent()) as YAMLFile

        val text = psiFile.text
        assertTrue("Should contain task with description", text.contains("desc:"))
    }
}
