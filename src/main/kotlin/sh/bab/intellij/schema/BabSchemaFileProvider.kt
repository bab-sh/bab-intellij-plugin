package sh.bab.intellij.schema

import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.jsonSchema.extension.JsonSchemaFileProvider
import com.jetbrains.jsonSchema.extension.JsonSchemaProviderFactory
import com.jetbrains.jsonSchema.extension.SchemaType
import sh.bab.intellij.filetype.BabFileType

class BabSchemaFileProvider : JsonSchemaFileProvider {

    override fun isAvailable(file: VirtualFile): Boolean = BabFileType.isBabfile(file)

    override fun getName(): String = "Babfile Schema"

    override fun getSchemaFile(): VirtualFile? =
        JsonSchemaProviderFactory.getResourceFile(javaClass, "/schemas/babfile.schema.json")

    override fun getSchemaType(): SchemaType = SchemaType.embeddedSchema
}
