import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature

plugins {
    application
}


// Define a custom task to find all JSON files
open class FindJsonFilesTask : DefaultTask() {
    init {
        group = "documentation"
        description = "Finds all component JSON files in the project and creates .mdx files."
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    class Properties {
        var controlType: String? = null
        var name: String? = null
        var description: String? = null
        var items: Array<Properties>? = null
        var properties: Array<Properties>? = null
        var label: String? = null
        var type: String? = null
        var required: Boolean? = false
        var options: Array<Option>? = null
        var optionsDataSource: OptionsDataSource? = null
        var dynamicPropertiesDataSource: DynamicPropertyDataSource? = null;

        private fun getFullTypeObject(): String? {
            val sb = StringBuilder()
            sb.append("{")
            if (properties != null) {
                for (proprety: Properties in properties!!) {
                    sb.append(proprety.getFullType(proprety.type))
                    if (proprety.name != null) sb.append("\\").append("(${proprety.name})")
                    sb.append(", ")
                }
                if (sb.length > 2) sb.replace(sb.length - 2, sb.length, "")
            }
//            if(sb.length>2)
            return sb.append("}").toString()
        }

        private fun getFullTypeArray(): String? {
            val sb = StringBuilder()
            sb.append("[")
            if (items != null) {
                for (proprety: Properties in items!!) {
                    sb.append(proprety.getFullType(proprety.type))
                    if (proprety.name != null) sb.append("\\").append("($${proprety.name})")
                    sb.append(", ")
                }
                if (sb.length > 2) sb.replace(sb.length - 2, sb.length, "")
            }
            return sb.append("]").toString()
        }

        private fun getFullType(type: String?): String? {
            return if (type.equals("OBJECT")) {
                getFullTypeObject()
            } else if (type.equals("ARRAY")) {
                getFullTypeArray()
            } else type;
        }

        fun getOutputString(): String {
            val typeDetails = getTypeDetails()
            val name2 = if (name == null) "" else name
            val description2 = if (description == null) "" else description

            return "| $name2 | $typeDetails | ${description2?.escapeHtml()} |"
        }

        override fun toString(): String {
            val typeDetails = getTypeDetails()

            return when {
                description == null -> formatWithoutDescription(name, label, typeDetails)
                else -> formatFull(name, label, typeDetails, description)
            }
        }

        private fun getTypeDetails(): String {
            val typeFull = getFullType(type)?.escapeHtml()
            return if (type == "OBJECT" || type == "ARRAY") {
                val detailsSummary = if (type == "OBJECT") "Properties" else "Items"
                "$type <details> <summary> $detailsSummary </summary> $typeFull </details>"
            } else if (type == "DYNAMIC_PROPERTIES") {
                if (dynamicPropertiesDataSource?.propertiesLookupDependsOn.isNullOrEmpty()) {
                    type.toString()
                } else {
                    val propertiesLookupDependsOn =
                        dynamicPropertiesDataSource?.propertiesLookupDependsOn?.joinToString(", ")
                    "$type <details> <summary> Depends On </summary> $propertiesLookupDependsOn </details>"
                }
            } else if (!options.isNullOrEmpty()) {
                val optionsString = options?.joinToString(", ") {
                    if (it.description != null)
                        "<span title=\"${it.description}\">${it.value.toString().escapeHtml()}</span>"
                    else
                        "<span>${it.value.toString().escapeHtml()}</span>"
                }
                "$type <details> <summary> Options </summary> $optionsString </details>"
            } else if (!optionsDataSource?.optionsLookupDependsOn.isNullOrEmpty()) {
                val optionsString = optionsDataSource?.optionsLookupDependsOn?.joinToString(", ")
                "$type <details> <summary> Depends On </summary> $optionsString </details>"
            } else {
                type.toString()
            }
        }

        fun String.escapeHtml(): String {
            return this
                .replace("&", "&amp;")
                .replace("{", "&#123;")
                .replace("}", "&#125;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
        }

        private fun formatWithoutDescription(name: String?, label: String?, typeDetails: String): String {
            return if (label == null) {
                "| $name | | $typeDetails |  | $required |"
            } else {
                "| $name | $label | $typeDetails |  | $required |"
            }
        }

        private fun formatFull(name: String?, label: String?, typeDetails: String, description: String?): String {
            val required2 = if (required == null) "false" else required.toString()
            return "| $name | $label | $typeDetails | ${description?.escapeHtml()} | $required2 |"
        }

        fun toJsonKeyValuePair(name: String?, type: String?, properties: Array<Properties>?, items: Array<Properties>?): String {
            val key = name;
            val value = getJsonValue(type, properties, items)
            return if (name.isNullOrEmpty()) "$value" else "\"$key\": $value"
        }

        private fun getJsonValue(type: String?, properties: Array<Properties>?, items: Array<Properties>?): Any {
            return when (type) {
                "ARRAY" -> getJsonArray(items)
                "BOOLEAN" -> false
                "DATE" -> "\"2021-01-01\""
                "DATE_TIME" -> "\"2021-01-01T00:00:00\""
                "DYNAMIC_PROPERTIES" -> "{}" // TODO
                "FILE_ENTRY" -> getJsonObject(properties)
                "INTEGER" -> 1
                "NUMBER" -> 0.0
                "OBJECT" -> getJsonObject(properties)
                "STRING" -> "\"\""
                "TIME" -> "\"00:00:00\""
                else -> {
                    "\"\""
                }
            }
        }

        private fun getJsonObject(properties: Array<Properties>?): String {
            val sb = StringBuilder()
            sb.append("{\n")
            if (properties != null) {
                for (property: Properties in properties) {
                    sb.append(property.toJsonKeyValuePair(property.name, property.type, property.properties, property.items))
                    sb.append(",\n")
                }

                if (sb.length > 2) {
                    sb.setLength(sb.length - 2)
                }
            }

            return sb.append("}").toString()
        }

        private fun getJsonArray(items: Array<Properties>?): String {
            val sb = StringBuilder()
            sb.append("[\n")
            if (items != null) {
                for (property: Properties in items) {
                    sb.append(getJsonValue(property.type, property.properties, property.items))
                    sb.append(",\n")
                }

                if (sb.length > 2) {
                    sb.setLength(sb.length - 2)
                }
            }

            return sb.append("]\n").toString()
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    class Option {
        var description: String? = null
        var label: String? = null
        var value: String? = null
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    class OptionsDataSource {
        var optionsLookupDependsOn: Array<String>? = null
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    class DynamicPropertyDataSource {
        var propertiesLookupDependsOn: Array<String>? = null
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    class OutputSchema {
        var controlType: String? = null
        var properties: Array<Properties>? = null
        var items: Array<Properties>? = null
        var type: String? = null

        private fun getPropertiesString(): String {
            return if (!properties.isNullOrEmpty()) {
                """
#### Properties

|     Name     |     Type     |     Description     |
|:------------:|:------------:|:-------------------:|
${properties?.joinToString("\n") { it.getOutputString() }}
"""
            } else if (!items.isNullOrEmpty() && items?.size == 1) {
                """
Items Type: ${items!![0].type}

${s()}
"""
            } else ""
        }

        private fun s(): String {

            return if (items!![0].type == "OBJECT") {
                """
#### Properties
|     Name     |     Type     |     Description     |
|:------------:|:------------:|:-------------------:|
${items!![0].properties?.joinToString("\n") { it.getOutputString() }}
"""
            } else ""
        }

        override fun toString(): String {
            return """
Type: $type

${getPropertiesString()}

"""
        }

        fun getOutputJson(): String {
            return if (!properties.isNullOrEmpty()) {
                """ {
                   ${properties?.joinToString(",\n") { it.toJsonKeyValuePair(it.name, it.type, it.properties, it.items) }}
                    }
               """.trimIndent()
            } else if (!items.isNullOrEmpty()) {
                """ [
                   ${items?.joinToString(",\n") { it.toJsonKeyValuePair(it.name, it.type, it.properties, it.items) }}
                    ]
               """.trimIndent()
            } else ""
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    class OutputResponse {
        var outputSchema: OutputSchema? = null
        var sampleOutput: Any? = null

        private fun getSampleOutputString(): String {
            if (sampleOutput == null) return ""

            return """
___Sample Output:___

```$sampleOutput```

"""
        }

        override fun toString(): String {
            return """
#### Output

${getSampleOutputString()}
$outputSchema
"""
        }

        fun getOutputJson(): String {
            if (outputSchema == null) {
                return ""
            }

            return outputSchema!!.getOutputJson()
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    class OutputDefinition {
        var outputResponse: OutputResponse? = null
//        var output: Any? = null

        private fun getOutputString(): String {
            return if (outputResponse != null) "$outputResponse"
            else return """
#### Output

The output for this action is dynamic and may vary depending on the input parameters. To determine the exact structure of the output, you need to execute the action.
"""
        }

        override fun toString(): String {
            return getOutputString();
        }

        fun getOutputJson(): String {
            return outputResponse?.getOutputJson() ?: "";
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    class Action {
        var description: String? = null
        var name: String? = null
        var outputDefinition: OutputDefinition? = null
        var properties: Array<Properties>? = null
        var title: String? = null
        var componentName: String? = null
        var componentVersion: Int? = null

        private fun getOutputDefinitionString(): String {
            if (outputDefinition == null) {
                return """
#### Output

This action does not produce any output.
"""
            }

            return "$outputDefinition"
        }

        override fun toString(): String {
            val propertiesSection = createPropertiesSection()
            val jsonExample = createJsonExample()
            val formattedJson = formatJson(jsonExample)

            return """
### $title
Name: $name

`$description`
$propertiesSection
#### Example JSON Structure
```json
$formattedJson
```
${getOutputDefinitionString()}
${createOutputJson()}
"""
        }

        private fun createJsonExample(): String {
            return if (properties.isNullOrEmpty()) {
                """ {
                    "label": "$title",
                    "name": "$name",
                    "type": "$componentName/v$componentVersion/$name"
                    }
               """.trimIndent()
            } else {
                """ {
                    "label": "$title",
                    "name": "$name",
                    "parameters": { ${properties?.joinToString(",\n") { it.toJsonKeyValuePair(it.name, it.type, it.properties, it.items) }} },
                    "type": "$componentName/v$componentVersion/$name" }
                """.trimIndent()
            }
        }

        private fun createOutputJson(): String {
            val outputJson = outputDefinition?.getOutputJson()

            return if (!outputJson.isNullOrEmpty()) {
                """
#### Output Example
```json
${formatJson(outputJson)}
```
""".trimIndent()
            } else {
                ""
            }
        }

        private fun formatJson(json: String): String {
            val mapper = ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT)
            val jsonNode = mapper.readTree(json)
            return mapper.writeValueAsString(jsonNode)
        }

        private fun createPropertiesSection(): String {
            return if (!properties.isNullOrEmpty()) {
                """
#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
${properties?.joinToString("\n")}
"""
            } else ""
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    class Trigger {
        var description: String? = null
        var name: String? = null
        var outputDefinition: OutputDefinition? = null
        var properties: Array<Properties>? = null
        var title: String? = null
        var type: String? = null
        var componentName: String? = null
        var componentVersion: Int? = null

        private fun getOutputResponseString(): String {
            if (outputDefinition == null) {
                return """
#### Output

This trigger does not produce any output.
"""
            }

            return "$outputDefinition"
        }

        override fun toString(): String {
            val jsonExample = createJsonExample()
            val formattedJson = formatJson(jsonExample)
            val propertiesSection = createPropertiesSection()

            return """
### $title
Name: $name

`$description`

Type: $type
$propertiesSection
${getOutputResponseString()}
#### JSON Example
```json
$formattedJson
```
"""
        }

        private fun createJsonExample(): String {
            return if (properties.isNullOrEmpty()) {
                """ {
                    "label": "$title",
                    "name": "$name",
                    "type": "$componentName/v$componentVersion/$name"
                    }
               """.trimIndent()
            } else {
                """ {
                    "label": "$title",
                    "name": "$name",
                    "parameters": { ${properties?.joinToString(",\n") { it.toJsonKeyValuePair(it.name, it.type, it.properties, it.items) }} },
                    "type": "$componentName/v$componentVersion/$name" }
                """.trimIndent()
            }
        }

        private fun formatJson(json: String): String {
            val mapper = ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT)
            val jsonNode = mapper.readTree(json)
            return mapper.writeValueAsString(jsonNode)
        }

        private fun createPropertiesSection(): String {
            return if (!properties.isNullOrEmpty()) {
                """
#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
${properties?.joinToString("\n")}
"""
            } else ""
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    class Authorizations {
        var name: String? = null
        var properties: Array<Properties>? = null
        var title: String? = null
        var type: String? = null

        override fun toString(): String {
            val label = if (title == null) name else title;
            return """
### $label

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
${properties?.joinToString("\n")}

"""
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    class Connection {
        var authorizations: Array<Authorizations>? = null
        var version: Int? = null

        override fun toString(): String {
            return """
## Connections

Version: $version

${authorizations?.joinToString("\n")}

"""
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    class ComponentCategory {
        var name: String? = null
        var label: String? = null

        override fun toString(): String {
            return "" + label
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    class Component {
        var actions: Array<Action>? = null
        var componentCategories: Array<ComponentCategory>? = null
        var connection: Connection? = null
        var customAction: Boolean? = null
        var description: String? = null
        var icon: String? = null
        var name: String? = null
        var tags: String? = null
        var title: String? = null
        var triggers: Array<Trigger>? = null
        var version: Int? = null
        var properties: Array<Properties>? = null

        private fun getCategoriesString(): String {
            if (componentCategories == null) {
                return ""
            }

            return """
Categories: ${componentCategories?.joinToString(", ")}
"""
        }

        private fun getConnectionString(): String {
            if (connection == null) {
                return ""
            }

            return """
$connection

<hr />
"""
        }

        private fun getTriggerString(): String {
            if (triggers.isNullOrEmpty()) {
                return ""
            }

            return """
## Triggers

${triggers?.joinToString("\n")}

"""
        }

        private fun getActionsString(): String {
            if (actions.isNullOrEmpty()) {
                return ""
            }

            return """
## Actions

${actions?.joinToString("\n")}
"""
        }

        private fun getCustomActionString(): String {
            return if (customAction == true) {
"""
## What to do if your action is not listed here?

If this component doesn't have the action you need, you can use **Custom Action** to create your own. Custom Actions empower you to define HTTP requests tailored to your specific requirements, allowing for greater flexibility in integrating with external services or APIs.

To create a Custom Action, simply specify the desired HTTP method, path, and any necessary parameters. This way, you can extend the functionality of your component beyond the predefined actions, ensuring that you can meet all your integration needs effectively.
"""
            } else {
                ""
            }
        }

        private fun getPropertiesString(): String {
            if (!properties.isNullOrEmpty()) {
                return """
## Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
${properties?.joinToString("\n")}
"""
            }
            return ""
        }

        override fun toString(): String {
            actions?.forEach { action ->
                action.componentName = name
                action.componentVersion = version
            }
            triggers?.forEach { trigger ->
                trigger.componentName = name
                trigger.componentVersion = version
            }

            return """---
title: "$title"
description: "$description"
---

${getCategoriesString()}

Type: $name/v$version

<hr />

${getPropertiesString()}
${getConnectionString()}
${getActionsString()}
${getTriggerString()}
${getCustomActionString()}
"""
        }
    }

    @TaskAction
    fun findJsonFiles() {
        val rootPath = project.rootDir.path
        val componentsPath = "$rootPath/docs/content/docs/reference/components"
        val taskDispatchersPath = "$rootPath/docs/content/docs/reference/flow-controls"
        val currentPath = project.projectDir.path

        if (currentPath.contains(Regex("/modules/.+/"))) {
            val definitionDir = File("$currentPath/src/test/resources/definition")
            val readmeFile = File("$currentPath/src/main/resources/README.mdx")

            if (definitionDir.exists() && definitionDir.isDirectory) {
                val mapper = ObjectMapper()

                val isComponentsDir = currentPath.contains("components")
                val moduleDocsDir = if (isComponentsDir) {
                    File(componentsPath)
                } else {
                    File(taskDispatchersPath)
                }

                if (!moduleDocsDir.exists()) {
                    moduleDocsDir.mkdirs()
                }

                val definitionJsonFiles = definitionDir.listFiles { file ->
                    file.isFile && file.extension.equals("json", ignoreCase = true)
                }?.toList().orEmpty()

                definitionJsonFiles.forEach { jsonFile ->
                    val jsonObject = mapper.readValue(jsonFile.readText(), Component::class.java)
                    val json = jsonObject.toString()

                    val path = when (isComponentsDir) {
                        true -> componentsPath
                        false -> taskDispatchersPath
                    }

                    val docsDir = File(path)
                    if (!docsDir.exists()) {
                        docsDir.mkdirs()
                    }

                    val mdFile = File(path, "${jsonFile.nameWithoutExtension}.mdx")
                    mdFile.writeText(json)

                    if (readmeFile.exists()) {
                        mdFile.appendText("<hr />\n\n# Additional Instructions\n\n")
                        mdFile.appendText(readmeFile.readText())
                    }
                }

                run {
                    val docsPath = if (isComponentsDir) componentsPath else taskDispatchersPath
                    val docsDir = File(docsPath)

                    if (docsDir.exists()) {
                        val expectedNames = definitionJsonFiles.map { it.nameWithoutExtension }.toSet()

                        val prefixes: Set<String> = if (expectedNames.isNotEmpty()) {
                            expectedNames.map { it.substringBefore("_v") }.toSet()
                        } else {
                            setOf(File(currentPath).name)
                        }

                        docsDir.listFiles { file -> file.isFile && file.extension.equals("mdx", ignoreCase = true) }
                            ?.forEach { mdxFile ->
                                val nameWithoutExt = mdxFile.nameWithoutExtension
                                val belongsToThisModule = prefixes.any { prefix ->
                                    nameWithoutExt == prefix || nameWithoutExt.startsWith("${prefix}_v")
                                }
                                val hasDefinition = expectedNames.contains(nameWithoutExt)

                                if (belongsToThisModule && !hasDefinition) {
                                    mdxFile.delete()
                                }
                            }
                    }
                }
            }
        }
    }
}

// Register the custom task with Gradle
tasks.register<FindJsonFilesTask>("generateDocumentation")
