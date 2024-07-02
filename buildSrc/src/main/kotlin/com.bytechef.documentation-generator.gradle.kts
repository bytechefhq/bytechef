
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper
//import org.gradle.internal.impldep.com.fasterxml.jackson.annotation.JsonIgnoreProperties
//import org.gradle.internal.impldep.com.fasterxml.jackson.databind.ObjectMapper

plugins {
    // Apply the common convention plugin for shared build configuration between library and application projects.
    id("com.bytechef.documentation-generator")

    // Apply the application plugin to add support for building a CLI application in Java.
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
        var name: String? = null
        var label: String? = null
        var type: String? = null
        var controlType: String? = null
        var items: Array<Properties>? = null

        override fun toString(): String {
            return "| $label | $type | $controlType  |"
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    class OutputSchema {
        var type: String? = null
        var controlType: String? = null
        var properties: Array<Properties>? = null

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
        var sampleOutput: Any? = null
        var outputSchema: OutputSchema? = null

        fun getSampleOutputString(): String {
            if(sampleOutput==null) return "";
            else return """
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
        var name: String? = null
        var description: String? = null
        var title: String? = null
        var properties: Array<Properties>? = null
        var outputResponse: OutputResponse? = null

        fun getOutputResponseString(): String {
            if (outputResponse == null) return ""
            else return "$outputResponse"
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
        var name: String? = null
        var description: String? = null
        var title: String? = null
        var type: String? = null
        var properties: Array<Properties>? = null
        var outputResponse: OutputResponse? = null

        fun getOutputResponseString(): String {
            if (outputResponse == null) return ""
            else return "$outputResponse"
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
        var type: String? = null
        var title: String? = null
        var properties: Array<Properties>? = null

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
        var version: Int? = null
        var authorizations: Array<Authorizations>? = null

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
        var categories: Array<String>? = null
        var description: String? = null
        var icon: String? = null
        var tags: String? = null
        var name: String? = null
        var version: Int? = null
        var title: String? = null
        var connection: Connection? = null
        var actions: Array<Action>? = null
        var triggers: Array<Trigger>? = null

        fun getTriggerString(): String {
            if(triggers==null) return "";
            else return """
# Triggers

${triggers?.joinToString("\n")}

<hr />
            """
        }

        fun getConnectionString(): String {
            if(connection==null) return "";
            else return """
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
        val docsPath = "docs/docs/reference/components/temp"
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
//                val json = mapper.writeValueAsString(jsonObject)

                val mdFileName = it.nameWithoutExtension.substringBefore("_") + ".md"
                val mdFile = File("$rootPath/$docsPath", mdFileName)
                mdFile.writeText(json)
            }
        } else {
            println("Components directory does not exist.")
        }
    }
}

// Register the custom task with Gradle
tasks.register<FindJsonFilesTask>("generateDocumentation")
