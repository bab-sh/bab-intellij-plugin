package sh.bab.plugin.schema

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.jsonSchema.extension.JsonSchemaFileProvider
import com.jetbrains.jsonSchema.extension.JsonSchemaProviderFactory
import com.jetbrains.jsonSchema.extension.SchemaType
import sh.bab.plugin.filetype.isBabfile

class BabSchemaProviderFactory : JsonSchemaProviderFactory {
    override fun getProviders(project: Project): List<JsonSchemaFileProvider> = listOf(BabSchemaProvider())
}

class BabSchemaProvider : JsonSchemaFileProvider {
    override fun isAvailable(file: VirtualFile): Boolean = isBabfile(file)
    override fun getName(): String = "Babfile"
    override fun getSchemaFile(): VirtualFile? = JsonSchemaProviderFactory.getResourceFile(javaClass, "/schemas/babfile.schema.json")
    override fun getSchemaType(): SchemaType = SchemaType.embeddedSchema
}
