
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
        private var controlType: String? = null
        private var name: String? = null
        private var items: Array<Properties>? = null
        private var label: String? = null
        private var type: String? = null

        override fun toString(): String {
            return "| $label | $type | $controlType  |"
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    class OutputSchema {
        private var controlType: String? = null
        private var properties: Array<Properties>? = null
        private var type: String? = null

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
        private var outputSchema: OutputSchema? = null
        private var sampleOutput: Any? = null

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
        private var description: String? = null
        private var name: String? = null
        private var outputResponse: OutputResponse? = null
        private var properties: Array<Properties>? = null
        private var title: String? = null

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
        private var description: String? = null
        private var name: String? = null
        private var outputResponse: OutputResponse? = null
        private var properties: Array<Properties>? = null
        private var title: String? = null
        private var type: String? = null

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
        private var name: String? = null
        private var properties: Array<Properties>? = null
        private var title: String? = null
        private var type: String? = null

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
        private var authorizations: Array<Authorizations>? = null
        private var version: Int? = null

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
        private var actions: Array<Action>? = null
        private var categories: Array<String>? = null
        private var connection: Connection? = null
        private var description: String? = null
        private var icon: String? = null
        private var name: String? = null
        private var tags: String? = null
        private var title: String? = null
        private var triggers: Array<Trigger>? = null
        private var version: Int? = null

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

        private fun getConnectionString(): String {
            if(connection==null) {
                return ""
            }

            return """
$connection

<hr />
            """
        }

        override fun toString(): String {
            return """
# $title
### $description
Categories: ${categories.contentToString()}

Version: $version

<hr />

${getConnectionString()}

${getTriggerString()}

# Actions

${actions?.joinToString("\n")}
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

                val docsPath = "$rootPath/$docsPath"

                val docsDir = File(docsPath)

                if (!docsDir.exists()) {
                    docsDir.mkdirs()
                }

                val mdFile = File(docsPath, mdFileName)

                mdFile.writeText(json)
            }
        } else {
            println("Components directory does not exist.")
        }
    }
}

// Register the custom task with Gradle
tasks.register<FindJsonFilesTask>("generateDocumentation")
