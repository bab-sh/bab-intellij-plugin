package sh.bab.intellij.schema

import com.intellij.openapi.project.Project
import com.jetbrains.jsonSchema.extension.JsonSchemaFileProvider
import com.jetbrains.jsonSchema.extension.JsonSchemaProviderFactory

class BabSchemaProviderFactory : JsonSchemaProviderFactory {
    override fun getProviders(project: Project): List<JsonSchemaFileProvider> =
        listOf(BabSchemaFileProvider())
}
