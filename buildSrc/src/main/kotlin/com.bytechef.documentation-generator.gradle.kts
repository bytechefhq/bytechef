
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
        var description: String? = null
        var items: Array<Properties>? = null
        var properties: Array<Properties>? = null
        var label: String? = null
        var type: String? = null

        private fun getFullTypeObject(): String? {
            val sb = StringBuilder()
            sb.append("{")
            if(properties!=null) {
                for (proprety: Properties in properties!!) {
                    sb.append(proprety.getFullType(proprety.type))
                    if(proprety.name!=null) sb.append("\\").append("(${proprety.name})")
                    sb.append(", ")
                }
                if(sb.length>2) sb.replace(sb.length-2, sb.length, "")
            }
//            if(sb.length>2)
            return sb.append("}").toString()
        }

        private fun getFullTypeArray(): String? {
            val sb = StringBuilder()
            sb.append("[")
            if(items!=null) {
                for (proprety: Properties in items!!) {
                    sb.append(proprety.getFullType(proprety.type))
                    if(proprety.name!=null) sb.append("\\").append("($${proprety.name})")
                    sb.append(", ")
                }
                if(sb.length>2) sb.replace(sb.length-2, sb.length, "")
            }
            return sb.append("]").toString()
        }

        private fun getFullType(type:String?): String? {
            return if(type.equals("OBJECT")){
                getFullTypeObject()
            } else if(type.equals("ARRAY")){
                getFullTypeArray()
            } else type;
        }

        override fun toString(): String {
            val typeFull = getFullType(type)

            return if (label==null) "| $typeFull | $controlType  |"
            else if(description==null) "| $label | $typeFull | $controlType  |  |"
            else "| $label | $typeFull | $controlType  |  $description  |"
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    class OutputSchema {
        var controlType: String? = null
        var properties: Array<Properties>? = null
        var items: Array<Properties>? = null
        var type: String? = null

        private fun getPropertiesString(): String {
            return if (properties != null) {
                """
#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
${properties?.joinToString("\n")}
"""
            } else if(items != null){
                """
#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
${items?.joinToString("\n")}
"""
            } else ""
        }

        override fun toString(): String {
            return """
Type: $type

${getPropertiesString()}

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
### $title
$description

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
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
### $title
$description

#### Type: $type
#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
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
### $title

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
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

        private fun getCategoriesString(): String {
            if(categories==null) {
                return ""
            }

            return """
Categories: ${categories.contentToString()}
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

        private fun getTriggerString(): String {
            if(triggers==null) {
                return ""
            }

            return """
## Triggers

${triggers?.joinToString("\n")}

<hr />
"""
        }

        private fun getActionsString(): String {
            if(actions==null) {
                return ""
            }

            return """
## Actions

${actions?.joinToString("\n")}
"""
        }

        override fun toString(): String {
            return """---
title: "$title"
description: "$description"
---
## Reference
<hr />

$description

${getCategoriesString()}

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
        val rootPath = project.rootDir.path
        val componentsPath = "$rootPath/docs/src/content/docs/reference/components"
        val taskDispatchersPath = "$rootPath/docs/src/content/docs/reference/task-dispatchers"
        val currentPath = project.projectDir.path

        if (currentPath.contains(Regex("/modules/.+/"))) {
            val name = currentPath.substringAfterLast("/")
            val jsonFile = File("$currentPath/src/test/resources/definition/${name}_v1.json")
            val readmeFile = File("$currentPath/src/main/resources/README.md")

            if(jsonFile.exists()){
                val mapper = ObjectMapper()
                val jsonObject = mapper.readValue(jsonFile.readText(), Component::class.java)
                val json = jsonObject.toString()

                val path = when (currentPath.contains("components")) {
                    true -> componentsPath
                    false -> taskDispatchersPath
                }

                val docsDir = File(path)
                if (!docsDir.exists()) {
                    docsDir.mkdirs()
                }

                val mdFile = File(path, "$name.md")
                mdFile.writeText(json)

                if (readmeFile.exists()) {
                    mdFile.appendText("<hr />\n\n# Additional instructions\n<hr />\n\n")
                    mdFile.appendText(readmeFile.readText())
                }
            }
        }
    }
}

// Register the custom task with Gradle
tasks.register<FindJsonFilesTask>("generateDocumentation")
