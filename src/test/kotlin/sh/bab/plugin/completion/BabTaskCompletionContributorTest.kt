package sh.bab.plugin.completion

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class BabTaskCompletionContributorTest : BasePlatformTestCase() {

    fun testCompletionInDepsField() {
        myFixture.configureByText("babfile.yml", """
            tasks:
              build:
                run:
                  - cmd: echo build
              test:
                run:
                  - cmd: echo test
              deploy:
                deps:
                  - <caret>
                run:
                  - cmd: echo deploy
        """.trimIndent())

        val completions = myFixture.completeBasic()
        assertNotNull("Expected completions", completions)
        val lookupStrings = completions?.map { it.lookupString } ?: emptyList()
        assertTrue("Expected 'build' in completions, got: $lookupStrings", lookupStrings.contains("build"))
        assertTrue("Expected 'test' in completions, got: $lookupStrings", lookupStrings.contains("test"))
    }

    fun testCompletionExcludesCurrentTask() {
        myFixture.configureByText("babfile.yml", """
            tasks:
              build:
                run:
                  - cmd: echo build
              test:
                deps:
                  - <caret>
                run:
                  - cmd: echo test
        """.trimIndent())

        val completions = myFixture.completeBasic()
        assertNotNull("Expected completions", completions)
        val lookupStrings = completions?.map { it.lookupString } ?: emptyList()
        assertTrue("Expected 'build' in completions", lookupStrings.contains("build"))
        assertFalse("Expected 'test' NOT in completions (self-reference)", lookupStrings.contains("test"))
    }

    fun testCompletionWithPartialInput() {
        myFixture.configureByText("babfile.yml", """
            tasks:
              build:
                run:
                  - cmd: echo build
              bundle:
                run:
                  - cmd: echo bundle
              test:
                deps:
                  - bu<caret>
                run:
                  - cmd: echo test
        """.trimIndent())

        val completions = myFixture.completeBasic()
        assertNotNull("Expected completions", completions)
        val lookupStrings = completions?.map { it.lookupString } ?: emptyList()
        assertTrue("Expected 'build' in completions", lookupStrings.contains("build"))
        assertTrue("Expected 'bundle' in completions", lookupStrings.contains("bundle"))
    }

    fun testCompletionShowsAllTasksWhenDepsIsEmpty() {
        myFixture.configureByText("babfile.yml", """
            tasks:
              task1:
                run:
                  - cmd: echo 1
              task2:
                run:
                  - cmd: echo 2
              task3:
                run:
                  - cmd: echo 3
              consumer:
                deps:
                  - <caret>
                run:
                  - cmd: echo consumer
        """.trimIndent())

        val completions = myFixture.completeBasic()
        assertNotNull("Expected completions", completions)
        val lookupStrings = completions?.map { it.lookupString } ?: emptyList()
        assertTrue("Expected 'task1' in completions", lookupStrings.contains("task1"))
        assertTrue("Expected 'task2' in completions", lookupStrings.contains("task2"))
        assertTrue("Expected 'task3' in completions", lookupStrings.contains("task3"))
        assertFalse("Expected 'consumer' NOT in completions", lookupStrings.contains("consumer"))
    }

    fun testCompletionInRunTaskField() {
        myFixture.configureByText("babfile.yml", """
            tasks:
              helper:
                run:
                  - cmd: echo helper
              main:
                run:
                  - task: <caret>
        """.trimIndent())

        val completions = myFixture.completeBasic()
        assertNotNull("Expected completions", completions)
        val lookupStrings = completions?.map { it.lookupString } ?: emptyList()
        assertTrue("Expected 'helper' in completions for run.task field", lookupStrings.contains("helper"))
    }

    fun testCompletionWithNamespacedTasks() {
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
                  - <caret>
                run:
                  - cmd: echo test
        """.trimIndent())

        val completions = myFixture.completeBasic()
        assertNotNull("Expected completions", completions)
        val lookupStrings = completions?.map { it.lookupString } ?: emptyList()
        assertTrue("Expected 'build:lint' in completions", lookupStrings.contains("build:lint"))
        assertTrue("Expected 'build:compile' in completions", lookupStrings.contains("build:compile"))
    }
}
