package sh.bab.plugin

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import sh.bab.plugin.filetype.BabFileType
import sh.bab.plugin.filetype.isBabfileName

class BabFileTypeTest : BasePlatformTestCase() {

    fun testBabFileTypeRecognition() {
        val psiFile = myFixture.configureByText("babfile.yml", "tasks:\n  build:\n    run:\n      - cmd: echo hello")
        assertEquals(BabFileType, psiFile.virtualFile.fileType)
    }

    fun testBabFileTypeYamlExtension() {
        val psiFile = myFixture.configureByText("babfile.yaml", "tasks:\n  test:\n    run:\n      - cmd: echo test")
        assertEquals(BabFileType, psiFile.virtualFile.fileType)
    }

    fun testBabFileTypeCaseInsensitive() {
        val psiFile = myFixture.configureByText("Babfile.yml", "tasks:\n  deploy:\n    run:\n      - cmd: echo deploy")
        assertEquals(BabFileType, psiFile.virtualFile.fileType)
    }

    fun testBabFileTypeWithMiddlePart() {
        val psiFile = myFixture.configureByText("babfile.dev.yml", "tasks:\n  dev:\n    run:\n      - cmd: echo dev")
        assertEquals(BabFileType, psiFile.virtualFile.fileType)
    }

    fun testBabFileTypeWithMiddlePartYaml() {
        val psiFile = myFixture.configureByText("babfile.prod.yaml", "tasks:\n  prod:\n    run:\n      - cmd: echo prod")
        assertEquals(BabFileType, psiFile.virtualFile.fileType)
    }

    fun testBabFileTypeWithMiddlePartCaseInsensitive() {
        val psiFile = myFixture.configureByText("Babfile.Local.yml", "tasks:\n  local:\n    run:\n      - cmd: echo local")
        assertEquals(BabFileType, psiFile.virtualFile.fileType)
    }

    fun testIsBabfileNamePatterns() {
        assertTrue(isBabfileName("babfile.yml"))
        assertTrue(isBabfileName("babfile.yaml"))
        assertTrue(isBabfileName("Babfile.yml"))
        assertTrue(isBabfileName("Babfile.yaml"))
        assertTrue(isBabfileName("BABFILE.YML"))
        assertTrue(isBabfileName("BABFILE.YAML"))

        assertTrue(isBabfileName("babfile.dev.yml"))
        assertTrue(isBabfileName("babfile.prod.yaml"))
        assertTrue(isBabfileName("Babfile.local.yml"))
        assertTrue(isBabfileName("Babfile.staging.yaml"))
        assertTrue(isBabfileName("babfile.my-env.yml"))

        assertFalse(isBabfileName("babfile.txt"))
        assertFalse(isBabfileName("notbabfile.yml"))
        assertFalse(isBabfileName("babfile"))
        assertFalse(isBabfileName("babfile.yml.bak"))
    }

    fun testBabFileTypeName() {
        assertEquals("Babfile", BabFileType.name)
    }

    fun testBabFileTypeDefaultExtension() {
        assertEquals("yml", BabFileType.defaultExtension)
    }
}