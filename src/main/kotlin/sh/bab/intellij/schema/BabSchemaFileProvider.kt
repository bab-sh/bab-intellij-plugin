package sh.bab.intellij.schema

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.jsonSchema.extension.JsonSchemaFileProvider
import com.jetbrains.jsonSchema.extension.JsonSchemaProviderFactory
import com.jetbrains.jsonSchema.extension.SchemaType

class BabSchemaFileProvider(private val project: Project) : JsonSchemaFileProvider {

    override fun isAvailable(file: VirtualFile): Boolean {
        val fileName = file.name.lowercase()
        return fileName == "babfile.yml" || fileName == "babfile.yaml"
    }

    override fun getName(): String = "Babfile Schema"

    override fun getSchemaFile(): VirtualFile? {
        return JsonSchemaProviderFactory.getResourceFile(
            BabSchemaFileProvider::class.java,
            "/schemas/babfile.schema.json"
        )
    }

    override fun getSchemaType(): SchemaType = SchemaType.embeddedSchema
}
