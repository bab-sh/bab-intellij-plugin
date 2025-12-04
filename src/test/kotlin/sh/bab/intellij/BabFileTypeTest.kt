package sh.bab.intellij

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import sh.bab.intellij.filetype.BabFileType

class BabFileTypeTest : BasePlatformTestCase() {

    fun testBabFileTypeRecognition() {
        val psiFile = myFixture.configureByText("babfile.yml", "tasks:\n  build:\n    run:\n      - cmd: echo hello")
        assertEquals(BabFileType.INSTANCE, psiFile.virtualFile.fileType)
    }

    fun testBabFileTypeYamlExtension() {
        val psiFile = myFixture.configureByText("babfile.yaml", "tasks:\n  test:\n    run:\n      - cmd: echo test")
        assertEquals(BabFileType.INSTANCE, psiFile.virtualFile.fileType)
    }

    fun testBabFileTypeCaseInsensitive() {
        val psiFile = myFixture.configureByText("Babfile.yml", "tasks:\n  deploy:\n    run:\n      - cmd: echo deploy")
        assertEquals(BabFileType.INSTANCE, psiFile.virtualFile.fileType)
    }

    fun testBabFileTypeName() {
        assertEquals("Babfile", BabFileType.INSTANCE.name)
    }

    fun testBabFileTypeDefaultExtension() {
        assertEquals("yml", BabFileType.INSTANCE.defaultExtension)
    }
}
