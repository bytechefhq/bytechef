
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper

plugins {
    application
}


// Define a custom task to find all JSON files
open class FindJsonFilesTask : DefaultTask() {
    init {
        group = "documentation"
        description = "Finds all component JSON files in the project and creates .md files."
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    class Properties {
        var controlType: String? = null
        var name: String? = null
        var items: Array<Properties>? = null
        var label: String? = null
        var type: String? = null

        override fun toString(): String {
            return "| $label | $type | $controlType  |"
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    class OutputSchema {
        var controlType: String? = null
        var properties: Array<Properties>? = null
        var type: String? = null

        override fun toString(): String {
            return """
Type: $type

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
${properties?.joinToString("\n")}

            """
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    class OutputResponse {
        var outputSchema: OutputSchema? = null
        var sampleOutput: Any? = null

        private fun getSampleOutputString(): String {
            if(sampleOutput==null) return ""

            return """
___Sample Output:___

```$sampleOutput```

"""
        }

        override fun toString(): String {
            return """
### Output

${getSampleOutputString()}
$outputSchema
            """
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    class Action {
        var description: String? = null
        var name: String? = null
        var outputResponse: OutputResponse? = null
        var properties: Array<Properties>? = null
        var title: String? = null

        private fun getOutputResponseString(): String {
            if (outputResponse == null) {
                return ""
            }

            return "$outputResponse"
        }

        override fun toString(): String {
            return """
## $title
### $description

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
${properties?.joinToString("\n")}

${getOutputResponseString()}
            """
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    class Trigger {
        var description: String? = null
        var name: String? = null
        var outputResponse: OutputResponse? = null
        var properties: Array<Properties>? = null
        var title: String? = null
        var type: String? = null

        private fun getOutputResponseString(): String {
            if (outputResponse == null) {
                return ""
            }

            return "$outputResponse"
        }

        override fun toString(): String {
            return """
## $title
### $description

#### Type: $type
#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
${properties?.joinToString("\n")}

${getOutputResponseString()}

            """
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    class Authorizations {
        var name: String? = null
        var properties: Array<Properties>? = null
        var title: String? = null
        var type: String? = null

        override fun toString(): String {
            return """
## $title

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
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
# Connections

Version: $version

${authorizations?.joinToString("\n")}

            """
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    class Component {
        var actions: Array<Action>? = null
        var categories: Array<String>? = null
        var connection: Connection? = null
        var description: String? = null
        var icon: String? = null
        var name: String? = null
        var tags: String? = null
        var title: String? = null
        var triggers: Array<Trigger>? = null
        var version: Int? = null

        private fun getConnectionString(): String {
            if(connection==null) {
                return ""
            }

            return """
$connection

<hr />
            """
        }

        private fun getTriggerString(): String {
            if(triggers==null) {
                return ""
            }

            return """
# Triggers

${triggers?.joinToString("\n")}

<hr />
            """
        }

        private fun getActionsString(): String {
            if(actions==null) {
                return ""
            }

            return """
# Actions

${actions?.joinToString("\n")}
            """
        }

        override fun toString(): String {
            return """---
title: $title
description: $description
---
### $description
Categories: ${categories.contentToString()}

Version: $version

<hr />

${getConnectionString()}

${getTriggerString()}

${getActionsString()}
            """
        }
    }

    @TaskAction
    fun findJsonFiles() {
        val componentsPath = "server/libs/modules/components"
        val docsPath = "docs/src/content/docs/reference/components"
        val rootPath = project.rootDir.path

        val componentsDir = File("$rootPath/$componentsPath")

        if (componentsDir.exists()) {
            val jsonFiles = componentsDir.walk().filter {
                it.isFile && it.extension == "json" && it.name.matches(Regex(".*_v1\\.json")) && !it.absolutePath.contains("/build/")
            }.toList()

            println("Found ${jsonFiles.size} JSON files:")

            jsonFiles.forEach {
                val mapper = ObjectMapper()
                val jsonObject = mapper.readValue(it.readText(), Component::class.java)

                val json = jsonObject.toString()

                val mdFileName = it.nameWithoutExtension.substringBefore("_") + ".md"
                val path = "$rootPath/$docsPath"

                val docsDir = File(path)

                if (!docsDir.exists()) {
                    docsDir.mkdirs()
                }

                val mdFile = File(path, mdFileName)
                mdFile.writeText(json)
            }
        } else {
            println("Components directory does not exist.")
        }
    }
}

// Register the custom task with Gradle
tasks.register<FindJsonFilesTask>("generateDocumentation")
